from osgeo import gdal
import sys, numpy
# this allows GDAL to throw Python Exceptions
gdal.UseExceptions()

try:
    raster = gdal.Open(sys.argv[1])
except RuntimeError, e:
    print 'Unable to open TIFF'
    print e
    sys.exit(1)

try:
    band = raster.GetRasterBand(1)
except RuntimeError, e:
    # for example, try GetRasterBand(10)
    print 'Band ( %i ) not found' % band_num
    print e
    sys.exit(1)


transform = raster.GetGeoTransform()
xOrigin = transform[0]
yOrigin = transform[3]
pixelWidth = transform[1]
pixelHeight = transform[5]

# print xOrigin, yOrigin, pixelWidth, pixelHeight

rasterdata = band.ReadAsArray().astype(numpy.float)
maskedrd = numpy.ma.masked_array(rasterdata, rasterdata == -1.0)


# Apply mask using list (way of explictly adding additional values to mask)
# http://stackoverflow.com/questions/11146229/creating-a-masked-array-in-python-with-multiple-given-values
# maskedrd = numpy.ma.array(rasterdata, mask=(~np.isfinite(a) | (a == -999)))
# maskedrd = numpy.ma.array(rasterdata, mask=numpy.logical_or.reduce([rasterdata == value for value in [-1.0, 0.0]]))

# print len(rasterdata)
# print numpy.mean(rasterdata), numpy.median(rasterdata), numpy.max(rasterdata), numpy.min(rasterdata)
# print numpy.ma.mean(maskedrd), numpy.ma.median(maskedrd), numpy.ma.max(maskedrd), numpy.ma.min(maskedrd)

ret = None
if type(numpy.ma.median(maskedrd)) is numpy.float64:
    ret = numpy.ma.median(maskedrd)
else:
    ret = numpy.ma.median(maskedrd)[0]

sys.stdout.write(str(ret))
sys.stdout.flush()
