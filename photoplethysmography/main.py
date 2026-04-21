from PyQt5.QtWidgets import QApplication

from mainwindow import MainWindow
from rppg import RPPG, get_heartbeat_filter

# CLASS TAKEN FROM SAMUEL PRÖLL 

if __name__ == "__main__":
    app = QApplication([])
    live_bandpass_filter = get_heartbeat_filter(order=4, cutoff=(0.7, 2), btype="bandpass", 
                                                fs=30, output="sos")
    rppg = RPPG(video=0, parent=app, filter_function=live_bandpass_filter)
    win = MainWindow(rppg=rppg)
    win.show()

    rppg.start()
    app.exec_()
    rppg.stop()
