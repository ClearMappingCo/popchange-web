#!/usr/bin/python
import sys
from qgis.core import *
from PyQt4.QtCore import QFileInfo, QSize
from PyQt4.QtGui import QImage, QColor, QPainter

# Example params:
# $ python raster_to_png.py /tmp/popchange/cache_d49703fba304409d315998b001c1ec9d.tiff /tmp/pc/c2.qml /tmp/pc/render2.png

# http://gis.stackexchange.com/questions/190404/how-to-render-layer-into-png-in-a-standalone-application

def main():
    # supply path to qgis install location
    QgsApplication.setPrefixPath("/usr/share/qgis", True)

    # create a reference to the QgsApplication, setting the
    # second argument to False disables the GUI
    qgs = QgsApplication([], False)
    
    # load providers
    qgs.initQgis()

    fileName = sys.argv[1]
    layerStyle = sys.argv[2]
    destinationFile = sys.argv[3]


    fileInfo = QFileInfo(fileName)
    baseName = fileInfo.baseName()
    rlayer = QgsRasterLayer(fileName, baseName)
    if not rlayer.isValid():
        print "Layer failed to load!"

    rlayer.loadNamedStyle(layerStyle)

    QgsMapLayerRegistry.instance().addMapLayer(rlayer)

    # create image
    minsize = 2048
    factor = 1
    w = rlayer.width()
    h = rlayer.height()

    if w < minsize or h < minsize:
        if h > w:
            factor = minsize / h
        else:
            factor = minsize / w

    img = QImage(QSize(w * factor, h * factor), QImage.Format_ARGB32_Premultiplied)

    # set image's background color (can get noise otherwise)
    color = QColor(255, 255, 255)
    img.fill(color.rgb())

    # create painter
    p = QPainter()
    p.begin(img)
    p.setRenderHint(QPainter.Antialiasing)
    
    render = QgsMapRenderer()
    
    # set layer set
    lst = [rlayer.id()]  # add ID of every layer
    render.setLayerSet(lst)
    
    # set extent
    rect = QgsRectangle(render.fullExtent())
    rect.scale(1.0)
    
    render.setExtent(rect)
    
    # set output size
    render.setOutputSize(img.size(), img.logicalDpiX())
    
    # do the rendering
    render.render(p)
    p.end()
    
    # save image
    img.save(destinationFile,"png")

    QgsApplication.exitQgis()


    
if __name__ == "__main__":
   main()

