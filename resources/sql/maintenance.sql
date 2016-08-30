-- :name manual-attrib-comparisons
-- :doc Manually created attribute comparisons grouped by source
SELECT src_attrib_id AS src, array_to_string(array_agg(dst_attrib_id), ',') AS dsts
FROM attrib_comparisons
WHERE auto_generated=FALSE
GROUP BY src_attrib_id


-- :name reversed-quality
-- :doc Comparison quality for reversed comparison
SELECT comparison_quality_id, description
FROM attrib_comparisons
WHERE src_attrib_id=:dst AND dst_attrib_id=:src;


-- :name create-attrib-comparison :!
-- :doc Create auto generated attribute comparison
INSERT INTO attrib_comparisons
(src_attrib_id, dst_attrib_id, auto_generated, comparison_quality_id, description, created)
SELECT :src, :dst, TRUE, :cid, :description, now()
WHERE NOT EXISTS (SELECT 1 FROM attrib_comparisons WHERE src_attrib_id=:src AND dst_attrib_id=:dst)


-- :name delete-auto-attrib-comparisons :!
-- :doc Delete the auto generated attribute comparisons
DELETE FROM attrib_comparisons WHERE auto_generated=TRUE;


-- :name attribs-all
-- :doc All attributes (e.g. for exporting to CSV)
SELECT * FROM attribs;


-- :name update-attrib :!
-- :doc Update attribute
UPDATE attribs SET cenus_year=:cyear, title=:title, modified=now() WHERE id=:id


-- :name update-attrib-files :!
-- :doc Update downloadable files for attrib
UPDATE attribs SET
       counts_filename=:counts-filename,
       counts_path=:counts-path,
       counts_lookup_filename=:counts-lookup-filename,
       counts_lookup_path=:counts-lookup-path,
       counts_lookup_types_filename=:counts-lookup-types-filename,
       counts_lookup_types_path=:counts-lookup-types-path,
       rates_filename=:rates-filename,
       rates_path=:rates-path,
       modified=now()
       WHERE id=:id


-- :name create-comparison :!
-- :doc Create comparison
INSERT INTO attrib_comparisons
(src_attrib_id, dst_attrib_id, auto_generated, comparison_quality_id, description, created)
VALUES (:src, :dst, FALSE, :cid, :description, now())
