#!/usr/bin/python
import sys
from qgis.core import *
from PyQt4.QtCore import QFileInfo

def main():        
    # supply path to qgis install location
    QgsApplication.setPrefixPath("/usr/share/qgis", True)

    # create a reference to the QgsApplication, setting the
    # second argument to False disables the GUI
    qgs = QgsApplication([], False)
    
    # load providers
    qgs.initQgis()
    
    # Write your code here to load some layers, use processing algorithms, etc.
    
    # raster = QFileInfo("/tmp/popchange/rastercalc_18162.tiff")
    # rasterBaseName = raster.baseName()
    rasterLayer = QgsRasterLayer(sys.argv[1], "rastercalc")
    if not rasterLayer.isValid():
        print "Layer failed to load!"
        
    provider = rasterLayer.dataProvider()
    renderer = QgsSingleBandGrayRenderer(provider,1)
    band = renderer.grayBand()
    
    myMin, myMax = provider.cumulativeCut(band, 0.02, 0.98)
    
    sys.stdout.write(str(myMin) + " " + str(myMax))
    sys.stdout.flush()
    
    # Tear down (stops seg fault)
    rasterLayer = None
    
    # When your script is complete, call exitQgis() to remove the provider and
    # layer registries from memory
    qgs.exitQgis()


if __name__ == "__main__":
   main()
