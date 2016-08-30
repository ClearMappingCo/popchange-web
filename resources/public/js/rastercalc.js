var rasterCanvasWrapperId = "rastercanvas";
var rasterCanvasControlsId = "rastercanvascontrols";
var rasterCanvasId = "raster";
var rasterUiId = "rasterui";

function renderPng(calcId) {
  // Transform from GeoTIFF EPSG:27700 to map projection (EPSG:3857)
  proj4.defs("EPSG:27700","+proj=tmerc +lat_0=49 +lon_0=-2 +k=0.9996012717 +x_0=400000 +y_0=-100000 +ellps=airy +towgs84=446.448,-125.157,542.06,0.15,0.247,0.842,-20.489 +units=m +no_defs");

  var attribution = new ol.control.Attribution({collapsible: true});
  var map = new ol.Map({
    target: document.getElementById(rasterCanvasWrapperId),
    renderer: 'canvas',
    controls: ol.control.defaults({attribution: false}).extend([attribution]),
    /*layers: [
      new ol.layer.Tile({
        source: new ol.source.OSM()
      }),
    ],*/
    view: new ol.View({
      // projection: 'EPSG:27700',
      center: ol.proj.transform(mapCentre, 'EPSG:27700', 'EPSG:3857'),
      // center: mapCentre,
      zoom: 1,
      // maxZoom: 13,
      // minZoom: 8
    })
  });

  // Do not smooth raster calculation
  map.on('precompose', function(evt) {
    evt.context.imageSmoothingEnabled = false;
    evt.context.webkitImageSmoothingEnabled = false;
    evt.context.mozImageSmoothingEnabled = false;
    evt.context.msImageSmoothingEnabled = false;
  });

  // alert (map.getView().getProjection().code);
  // console.log(map.getView().getProjection());

  var rasterCalculationExtent = ol.extent.applyTransform(        
    [rasterTopLeft[0], rasterTopLeft[1], rasterBottomRight[0], rasterBottomRight[1]],
    ol.proj.getTransform("EPSG:27700", "EPSG:3857")
  );
      

  // Draw static image layer
  var imageLayer = new ol.layer.Image({
    opacity: 1.0,
    source: new ol.source.ImageStatic({
      url: "/raster-calc-png?id="+calcId, // TODO Get from var so context aware
      
      projection: map.getView().getProjection(),
      // projection:new OpenLayers.Projection('EPSG:900913'),
      /**/
      imageExtent: rasterCalculationExtent
      
      /*imageExtent:  
        [rasterTopLeft[0], rasterTopLeft[1], rasterBottomRight[0], rasterBottomRight[1]]*/
      
    })
  });

  map.addLayer(imageLayer);

  // Zoom to extend of raster calculation
  map.getView().fit(rasterCalculationExtent, map.getSize());
  

  // Update with current value of opacity
  $("#opacity-slider").slider("setValue", imageLayer.getOpacity());

  // Bind to update layer opacity
  $("#opacity-slider").slider().on("slide", function(ev) {
    imageLayer.setOpacity(ev.value)
  });

  $("html, body").animate({
    scrollTop: $("#"+rasterCanvasControlsId).offset().top
  }, 500);

  $("#form-submit").prop("disabled", false);
}

function initRasterCalcVisual(calcId) {
  $("#"+rasterCanvasWrapperId).empty();
  renderPng(calcId);
}

function initRasterCalc(calcId) {
  $("#"+rasterCanvasWrapperId).removeClass("hidden");
  $("#"+rasterCanvasControlsId).removeClass("hidden");
  $("#rasterlegend").removeClass("hidden");
  $("#rasterexports").removeClass("hidden");

  $("#opacity-slider").slider();

  $("#rasterlegend").load("/visualisation-legend?id="+calcId);
  $("#rasterexports").load(
    "/visualisation-exports?id="+calcId +
      "&set1="+$("#set1-attrib").val() +
      "&set2="+$("#set2-attrib").val() +
      "&area="+$("#area").val() +
      "&countsorrates="+$("input[name=countsorrates]:checked", "#countsorrates").val()
  );

  $.getScript("/visualisation-js?id="+calcId);
}

function initAltForm() {
  $("#"+rasterUiId).removeClass("hidden");
  $("#non-js-form").remove();

  // Event handlers...

  // Load attribs for set 1
  $("#set1-year").on('hidden.bs.select', function (e) {
    $("#set1-attrib").load("/set-attribs?year="+$("#set1-year").val(), function() {
      $("#set1-attrib").prop("disabled", false);
      $("#set1-attrib").selectpicker('refresh');
    });
  });

  // Load years for set 2
  $("#set1-attrib").on('hidden.bs.select', function (e) {
    $("#set2-year").load("/set2-years?id="+$("#set1-attrib").val(), function() {
      $("#set2-year").prop("disabled", false);
      $("#set2-year").selectpicker('refresh');
    });
  });

  // Load attribs for set 2
  $("#set2-year").on('hidden.bs.select', function (e) {
    $("#set2-attrib").load("/set2-attribs?id="+$("#set1-attrib").val()+"&year="+$("#set2-year").val(), function() {
      $("#set2-attrib").prop("disabled", false);
      $("#set2-attrib").selectpicker('refresh');
    });
  });

  // Load comparison quality for set 2
  $("#set2-attrib").on('hidden.bs.select', function(e) {
    $("#comparisonquality").load("/comparison-quality?src="+$("#set1-attrib").val()+"&dst="+$("#set2-attrib").val());
    $("#countsorrates").load("/counts-or-rates?src="+$("#set1-attrib").val()+"&dst="+$("#set2-attrib").val());
  });

  $("#form-submit").click(function() {
    $("#form-submit").prop("disabled", true);
    
    var data = {"__anti-forgery-token": $("#__anti-forgery-token").val(),
                "area": $("#area").val(),
                "set1": $("#set1-attrib").val(),
                "set2": $("#set2-attrib").val(),
                "countsorrates": $("input[name=countsorrates]:checked", "#countsorrates").val()
               };

    if($("#excl-lcc").prop("checked")) {
      data["excl-lcc"] = "on";
    }
    
    $.post("/raster-calc-gen-alt", data, function(resp) {
      initRasterCalc(resp);
    });
  });

  // Load initial data
  $("#set1-year").load("/set-years", function() {
    $("#set1-year").prop('disabled', false);
    $("#set1-year").selectpicker('refresh');
  });
}

$(document).ready(function() {
  // Hide non-js elements
  $(".non-js").addClass("hidden");
  initAltForm();
});
