"""Desktop entry: PySide6 GUI for dietary restrictions and label OCR."""

from __future__ import annotations

import sys

import cv2
import numpy as np
from PySide6.QtCore import Qt, QTimer
from PySide6.QtGui import QImage, QPixmap
from PySide6.QtWidgets import (
    QApplication,
    QCheckBox,
    QFileDialog,
    QDialog,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QMessageBox,
    QMainWindow,
    QProgressDialog,
    QPushButton,
    QSplitter,
    QTextEdit,
    QVBoxLayout,
    QWidget,
)

from src.constants import ALLERGEN_OPTIONS, DISCLAIMER
from src.match import MatchResult, load_default_rules_dir
from src.ocr import get_easyocr_reader
from src.preferences import parse_extra_avoid
from src.scan_service import scan_label_image, summarize_result

RULES_DIR = load_default_rules_dir()


class WebcamDialog(QDialog):
    """Live preview and single-frame capture."""

    def __init__(self, parent: QWidget | None = None) -> None:
        super().__init__(parent)
        self.setWindowTitle("Capture label")
        self.resize(720, 520)
        self._captured: bytes | None = None
        self._rgb_buffer: np.ndarray | None = None

        self._cap = self._open_camera()

        layout = QVBoxLayout(self)
        self._video = QLabel("No camera")
        self._video.setMinimumSize(640, 360)
        self._video.setAlignment(Qt.AlignCenter)
        self._video.setStyleSheet("background:#222;color:#aaa;")
        layout.addWidget(self._video)

        btn_row = QHBoxLayout()
        self._snap = QPushButton("Capture photo")
        self._snap.clicked.connect(self._on_capture)
        cancel = QPushButton("Cancel")
        cancel.clicked.connect(self.reject)
        btn_row.addWidget(self._snap)
        btn_row.addWidget(cancel)
        layout.addLayout(btn_row)

        if self._cap.isOpened():
            self._timer = QTimer(self)
            self._timer.timeout.connect(self._tick)
            self._timer.start(33)
        else:
            QMessageBox.warning(
                self,
                "Camera",
                "Could not open a webcam. Connect a camera or use Open image instead.",
            )

    @staticmethod
    def _open_camera() -> cv2.VideoCapture:
        if sys.platform == "win32":
            cap = cv2.VideoCapture(0, cv2.CAP_DSHOW)
            if cap.isOpened():
                return cap
            cap.release()
        return cv2.VideoCapture(0)

    def _tick(self) -> None:
        if not self._cap.isOpened():
            return
        ok, frame = self._cap.read()
        if not ok:
            return
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        self._rgb_buffer = np.ascontiguousarray(rgb)
        h, w, ch = self._rgb_buffer.shape
        qimg = QImage(
            self._rgb_buffer.data,
            w,
            h,
            ch * w,
            QImage.Format_RGB888,
        )
        pix = QPixmap.fromImage(qimg).scaled(
            self._video.size(),
            Qt.KeepAspectRatio,
            Qt.SmoothTransformation,
        )
        self._video.setPixmap(pix)

    def _on_capture(self) -> None:
        if not self._cap.isOpened():
            self.reject()
            return
        ok, frame = self._cap.read()
        if ok:
            ok_enc, buf = cv2.imencode(".jpg", frame, [int(cv2.IMWRITE_JPEG_QUALITY), 92])
            if ok_enc:
                self._captured = buf.tobytes()
        self.accept()

    def captured_bytes(self) -> bytes | None:
        return self._captured

    def closeEvent(self, event) -> None:  # noqa: N802
        if hasattr(self, "_timer"):
            self._timer.stop()
        if self._cap.isOpened():
            self._cap.release()
        super().closeEvent(event)


class MainWindow(QMainWindow):
    def __init__(self) -> None:
        super().__init__()
        self.setWindowTitle("Ingredient checker")
        self.resize(1100, 720)
        self._image_bytes: bytes | None = None
        self._ocr_reader = None

        central = QWidget()
        self.setCentralWidget(central)
        root = QHBoxLayout(central)

        splitter = QSplitter(Qt.Horizontal)
        root.addWidget(splitter)

        # Left: restrictions
        left = QWidget()
        left_l = QVBoxLayout(left)
        grp = QGroupBox("Your restrictions")
        g = QVBoxLayout(grp)
        self._allergen_checks: dict[str, QCheckBox] = {}
        for key, label in ALLERGEN_OPTIONS:
            cb = QCheckBox(label)
            self._allergen_checks[key] = cb
            g.addWidget(cb)
        self._vegan = QCheckBox("Vegan")
        self._vegetarian = QCheckBox("Vegetarian")
        g.addWidget(self._vegan)
        g.addWidget(self._vegetarian)
        g.addWidget(QLabel("Additional terms to avoid (comma or line separated):"))
        self._extra = QTextEdit()
        self._extra.setPlaceholderText("e.g. coconut, mustard")
        self._extra.setMaximumHeight(100)
        g.addWidget(self._extra)
        left_l.addWidget(grp)

        disc = QLabel(DISCLAIMER)
        disc.setWordWrap(True)
        disc.setStyleSheet("color:#666;font-size:11px;")
        left_l.addWidget(disc)
        left_l.addStretch()

        # Right: image + actions + results
        right = QWidget()
        rr = QVBoxLayout(right)

        img_row = QHBoxLayout()
        self._open_btn = QPushButton("Open image…")
        self._open_btn.clicked.connect(self._open_file)
        self._cam_btn = QPushButton("Webcam…")
        self._cam_btn.clicked.connect(self._open_webcam)
        self._analyze_btn = QPushButton("Analyze label")
        self._analyze_btn.clicked.connect(self._analyze)
        self._analyze_btn.setEnabled(False)
        img_row.addWidget(self._open_btn)
        img_row.addWidget(self._cam_btn)
        img_row.addWidget(self._analyze_btn)
        rr.addLayout(img_row)

        self._preview = QLabel("No image loaded")
        self._preview.setMinimumHeight(280)
        self._preview.setAlignment(Qt.AlignCenter)
        self._preview.setStyleSheet("background:#2a2a2a;color:#888;border:1px solid #444;")
        rr.addWidget(self._preview)

        self._summary = QLabel("")
        self._summary.setWordWrap(True)
        rr.addWidget(self._summary)

        self._results = QTextEdit()
        self._results.setReadOnly(True)
        self._results.setPlaceholderText("Results appear here after Analyze.")
        rr.addWidget(self._results, stretch=1)

        splitter.addWidget(left)
        splitter.addWidget(right)
        splitter.setStretchFactor(0, 0)
        splitter.setStretchFactor(1, 1)
        splitter.setSizes([320, 780])

        self.statusBar().showMessage('Tip: first analysis loads OCR — may take a moment.')

    def showEvent(self, event) -> None:  # noqa: N802
        super().showEvent(event)
        if self._ocr_reader is None:
            QTimer.singleShot(100, self._preload_ocr)

    def _preload_ocr(self) -> None:
        self.statusBar().showMessage("Loading OCR engine (first launch may download models)…")
        QApplication.processEvents()
        try:
            self._ocr_reader = get_easyocr_reader()
            self.statusBar().showMessage("Ready.")
        except Exception as e:
            self.statusBar().showMessage("OCR failed to load.")
            QMessageBox.critical(
                self,
                "OCR error",
                f"Could not initialize EasyOCR:\n{e}\n\nInstall dependencies with: pip install -r requirements.txt",
            )

    def _open_file(self) -> None:
        path, _ = QFileDialog.getOpenFileName(
            self,
            "Open label image",
            "",
            "Images (*.png *.jpg *.jpeg *.webp);;All files (*.*)",
        )
        if not path:
            return
        try:
            with open(path, "rb") as f:
                self._image_bytes = f.read()
        except OSError as e:
            QMessageBox.warning(self, "Open file", str(e))
            return
        self._show_preview(self._image_bytes)
        self._analyze_btn.setEnabled(True)

    def _open_webcam(self) -> None:
        dlg = WebcamDialog(self)
        if dlg.exec() == QDialog.Accepted:
            data = dlg.captured_bytes()
            if data:
                self._image_bytes = data
                self._show_preview(data)
                self._analyze_btn.setEnabled(True)

    def _show_preview(self, data: bytes) -> None:
        pix = QPixmap()
        if pix.loadFromData(data):
            scaled = pix.scaled(
                self._preview.size(),
                Qt.KeepAspectRatio,
                Qt.SmoothTransformation,
            )
            self._preview.setPixmap(scaled)
        else:
            self._preview.setText("Could not decode image")

    def resizeEvent(self, event) -> None:  # noqa: N802
        super().resizeEvent(event)
        if self._image_bytes:
            self._show_preview(self._image_bytes)

    def _selected_allergens(self) -> list[str]:
        return [k for k, cb in self._allergen_checks.items() if cb.isChecked()]

    def _analyze(self) -> None:
        if not self._image_bytes:
            QMessageBox.information(self, "Analyze", "Load an image or capture from webcam first.")
            return
        if self._ocr_reader is None:
            QMessageBox.warning(self, "Analyze", "OCR engine is not ready yet.")
            return

        progress = QProgressDialog("Reading text from image…", None, 0, 0, self)
        progress.setWindowTitle("Please wait")
        progress.setWindowModality(Qt.WindowModal)
        progress.show()
        QApplication.processEvents()

        try:
            payload = scan_label_image(
                self._image_bytes,
                rules_dir=RULES_DIR,
                selected_allergens=self._selected_allergens(),
                vegan=self._vegan.isChecked(),
                vegetarian=self._vegetarian.isChecked(),
                extra_avoid=parse_extra_avoid(self._extra.toPlainText()),
                ocr_reader=self._ocr_reader,
            )
        except Exception as e:
            progress.close()
            QMessageBox.critical(self, "OCR failed", str(e))
            return
        progress.close()

        result = MatchResult(
            raw_text=payload["raw_text"],
            normalized=payload["normalized"],
            violations=payload["violations"],
            warnings=payload["warnings"],
            may_contain_notices=payload["may_contain_notices"],
        )
        self._render_results(result)

    def _render_results(self, result: MatchResult) -> None:
        lines: list[str] = []

        summary, level = summarize_result(result)
        if level == "no_text":
            self._summary.setText(summary)
            self._summary.setStyleSheet("color:#c0392b;font-weight:bold;")
            self._results.clear()
            self._results.append("No OCR output.")
            return

        if level == "violation":
            self._summary.setText(summary)
            self._summary.setStyleSheet("color:#c0392b;font-weight:bold;")
        elif level == "warning":
            self._summary.setText(summary)
            self._summary.setStyleSheet("color:#b8860b;font-weight:bold;")
        else:
            self._summary.setText(summary)
            self._summary.setStyleSheet("color:#27ae60;font-weight:bold;")

        if result.may_contain_notices:
            lines.append("May contain / facility wording:")
            for m in result.may_contain_notices:
                lines.append(f"  • {m}")
            lines.append("")

        if result.violations:
            lines.append("Violations:")
            for v in result.violations:
                lines.append(f"  • {v['category']}: matched “{v['keyword']}”")
            lines.append("")

        if result.warnings:
            lines.append("Review manually:")
            for w in result.warnings:
                lines.append(f"  • {w['term']}: {w['reason']}")
            lines.append("")

        lines.append("— Extracted text (OCR) —")
        lines.append(result.raw_text)

        self._results.setPlainText("\n".join(lines))


def main() -> None:
    app = QApplication(sys.argv)
    app.setApplicationName("Ingredient checker")
    win = MainWindow()
    win.show()
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
