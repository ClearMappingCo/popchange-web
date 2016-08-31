-- src/popchange/db/sql/raster-calculation.sql
-- PopChange Raster Calculation

-- :name ag-tables
-- :doc ASCII grid tables
SELECT table_name AS table
FROM information_schema.tables
WHERE table_schema='public' AND table_name LIKE 'ag_%'
ORDER BY table_name;

-- :name clip-areas
-- :doc Areas to clip raster calculations by
SELECT id, title FROM clip_areas ORDER BY title;

-- :name clip-area-shapefile-table :1
-- :doc Table containing shapefile for clip area
SELECT shapefile_table FROM clip_areas WHERE id=:id

-- :name clip-areas-by-id :? :*
-- :doc Area to clip by id
SELECT id, title FROM clip_areas WHERE id IN(:v*:ids);

-- :name set-years
-- :doc Attribute set years
SELECT DISTINCT cenus_year FROM attribs ORDER BY cenus_year DESC;

-- :name set-year :1
-- :doc Attribute set year
SELECT cenus_year FROM attribs WHERE counts_table=:tbl OR rates_table=:tbl LIMIT 1

-- :name set-attribs
-- :doc Available attributes for year
SELECT id, title FROM attribs WHERE cenus_year=:cyear ORDER BY title

-- :name set2-years
-- :doc Years for second attribute set
SELECT DISTINCT cenus_year
FROM attrib_comparisons ac
INNER JOIN attribs a
ON ac.dst_attrib_id=a.id
WHERE ac.src_attrib_id=:aid
-- UNION
-- SELECT DISTINCT cenus_year
-- FROM attrib_comparisons ac
-- INNER JOIN attribs a
-- ON ac.src_attrib_id=a.id
-- WHERE ac.dst_attrib_id=:aid
-- UNION
-- SELECT DISTINCT cenus_year
-- FROM attrib_comparisons ac1
-- INNER JOIN attrib_comparisons ac2
-- ON ac1.src_attrib_id=ac2.src_attrib_id
-- INNER JOIN attribs a
-- ON ac2.dst_attrib_id=a.id
-- WHERE ac1.dst_attrib_id=:aid AND a.id!=:aid
 ORDER BY cenus_year DESC

-- :name set2-attribs
-- :doc Attributes for second attribute set with quality indication
SELECT DISTINCT a.id, a.title, cq.colour, cq.ordering
FROM attrib_comparisons ac
INNER JOIN attribs a
ON ac.dst_attrib_id=a.id
INNER JOIN comparison_qualities cq
ON ac.comparison_quality_id=cq.id
WHERE ac.src_attrib_id=:aid AND a.cenus_year=:cyear
-- UNION
-- SELECT DISTINCT a.id, a.title, cq.colour
-- FROM attrib_comparisons ac
-- INNER JOIN attribs a
-- ON ac.src_attrib_id=a.id
-- INNER JOIN comparison_qualities cq
-- ON ac.comparison_quality_id=cq.id
-- WHERE ac.dst_attrib_id=:aid AND a.cenus_year=:cyear
-- UNION
-- SELECT DISTINCT a.id, a.title, cq.colour
-- FROM attrib_comparisons ac1
-- INNER JOIN attrib_comparisons ac2
-- ON ac1.src_attrib_id=ac2.src_attrib_id
-- INNER JOIN attribs a
-- ON ac2.dst_attrib_id=a.id
-- INNER JOIN comparison_qualities cq
-- ON ac2.comparison_quality_id=cq.id
-- WHERE ac1.dst_attrib_id=:aid AND a.id!=:aid AND a.cenus_year=:cyear
ORDER BY cq.ordering

-- :name attribs-by-id :? :*
-- :doc Attributes by id
SELECT id, title, cenus_year FROM attribs WHERE id IN (:v*:ids);


-- :name attrib-counts-tables
-- :doc Count table names for attributes
SELECT
        (SELECT counts_table FROM attribs WHERE id=:a1) AS set1,
        (SELECT counts_table FROM attribs WHERE id=:a2) AS set2


-- :name attrib-rates-tables
-- :doc Rate table names for attributes
SELECT
        (SELECT rates_table FROM attribs WHERE id=:a1) AS set1,
        (SELECT rates_table FROM attribs WHERE id=:a2) AS set2


-- :name all-attribs-have-rates-tables :? :1
-- :doc All atribbutes have rates tables
SELECT CASE WHEN COUNT(*)=0 THEN true ELSE false END AS tf FROM attribs WHERE rates_table IS NULL AND id IN (:v*:ids);


-- :name attribs-paths
-- :doc Paths for attrib
SELECT id, cenus_year, title, counts_path, rates_path, counts_lookup_path, counts_lookup_types_path FROM attribs WHERE id IN (:v*:ids);


-- :name comparison-quality
-- :doc Comparison quality for attribute relationship
SELECT CASE WHEN ac.description!='' THEN ac.description ELSE cq.title END AS description, cq.colour
FROM attrib_comparisons ac
INNER JOIN comparison_qualities cq
ON ac.comparison_quality_id=cq.id
WHERE ac.src_attrib_id=:src AND ac.dst_attrib_id=:dst

-- :name lo-create-raster-calc :? :1
-- :doc Create Raster Calculation visual in database's large object store
SELECT oid, lowrite(lo_open(oid, 131072), tiff) As num_bytes
FROM (
     VALUES (lo_create(0), ST_AsTIFF( (

            SELECT ST_MapAlgebra(t1.rast, t2.rast, '[rast1] - [rast2]')
            FROM :i:set1 t1, :i:set2 t2
            WHERE t1.rid=1 AND t2.rid=1
            
            ) ) 
     ) ) As v(oid,tiff);


-- :name lo-create-raster-calc-clipped :? :1
-- :doc Create clipped Raster Calculation visual in database's large object store
SELECT oid, lowrite(lo_open(oid, 131072), tiff) As num_bytes
FROM (
     VALUES (lo_create(0), ST_AsTIFF( (

            SELECT ST_Clip(ST_MapAlgebra(t1.rast, t2.rast, '[rast1] - [rast2]'), s1.geom)
            FROM :i:set1 t1, :i:set2 t2, :i:area s1
            WHERE t1.rid=1 AND t2.rid=1 AND s1.gid=1
            
            ) ) 
     ) ) As v(oid,tiff);


-- :name lo-create-raster-calc-excl-low-count-cells :? :1
-- :doc Create Raster Calculation visual in database's large object store
WITH masked AS (
     SELECT 1 AS rid, ST_MapAlgebra(t1.rast, m1.rast, '[rast1] - [rast2]') AS rast
     FROM :i:set1 t1, :i:mask1 m1
     WHERE t1.rid=1 AND m1.rid=1

     UNION ALL

     SELECT 2 AS rid, ST_MapAlgebra(t2.rast, m2.rast, '[rast1] - [rast2]') AS rast
     FROM :i:set2 t2, :i:mask2 m2
     WHERE t2.rid=1 AND m2.rid=1
)

SELECT oid, lowrite(lo_open(oid, 131072), tiff) As num_bytes
FROM (
     VALUES (lo_create(0), ST_AsTIFF( (

            SELECT ST_MapAlgebra(masked1.rast, masked2.rast, '[rast1] - [rast2]')
            FROM masked masked1
            CROSS JOIN masked masked2
            WHERE masked1.rid=1 AND masked2.rid=2
            
            ) ) 
     ) ) As v(oid,tiff);


-- :name lo-create-raster-calc-clipped-excl-low-count-cells :? :1
-- :doc Create Raster Calculation visual in database's large object store
WITH masked AS (
     SELECT 1 AS rid, ST_MapAlgebra(t1.rast, m1.rast, '[rast1] - [rast2]') AS rast
     FROM :i:set1 t1, :i:mask1 m1
     WHERE t1.rid=1 AND m1.rid=1

     UNION ALL

     SELECT 2 AS rid, ST_MapAlgebra(t2.rast, m2.rast, '[rast1] - [rast2]') AS rast
     FROM :i:set2 t2, :i:mask2 m2
     WHERE t2.rid=1 AND m2.rid=1
)

SELECT oid, lowrite(lo_open(oid, 131072), tiff) As num_bytes
FROM (
     VALUES (lo_create(0), ST_AsTIFF( (

            SELECT ST_Clip(ST_MapAlgebra(masked1.rast, masked2.rast, '[rast1] - [rast2]'), s1.geom)
            FROM masked masked1, :i:area s1
            CROSS JOIN masked masked2
            WHERE masked1.rid=1 AND masked2.rid=2 AND s1.gid=1
            
            ) ) 
     ) ) As v(oid,tiff);


-- :name lo-readall :? :1
-- :doc Return large object converted to bytea
SELECT lo_readall(:oid) AS dat;


-- :name lo-unlink :? :1
-- :doc Unlink the object from the database's large object store
SELECT lo_unlink(:oid);


-- :name lo-unlink-all :? :*
-- :doc Unlink all objects from database's large object store
SELECT lo_unlink(l.oid) FROM pg_largeobject_metadata l;



-- :name create-lo-readall-function :!
-- :doc Create lo_readall() function to convert large object to bytea in database
-- http://postgresql.nabble.com/GENERAL-Large-Object-to-Bytea-Conversion-td1870627.html
CREATE FUNCTION lo_readall(oid) RETURNS bytea
        AS $_$

SELECT loread(q3.fd, q3.filesize + q3.must_exec) FROM
        (SELECT q2.fd, q2.filesize, lo_lseek(q2.fd, 0, 0) AS must_exec FROM
                (SELECT q1.fd, lo_lseek(q1.fd, 0, 2) AS filesize FROM
                        (SELECT lo_open($1, 262144) AS fd)
                AS q1)
        AS q2)
AS q3

$_$ LANGUAGE sql STRICT;
