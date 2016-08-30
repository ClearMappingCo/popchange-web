-- :name create-user-login :!
-- :doc Log user login
INSERT INTO log_user_logins (user_id, remote_ip, created) VALUES (:uid, :ip::inet, now());


-- :name create-attrib-comparison :!
-- :doc Log attribute comparison
INSERT INTO log_attrib_comparisons (user_id, src_attrib_id, src_attrib_census_year, src_attrib_title, dst_attrib_id, dst_attrib_census_year, dst_attrib_title,  exclude_low_count_cells, counts_table, remote_ip, created) VALUES
       (:uid,
             :src-attrib-id,
             (SELECT cenus_year FROM attribs WHERE id=:src-attrib-id),
             (SELECT title FROM attribs WHERE id=:src-attrib-id),
             :dst-attrib-id,
             (SELECT cenus_year FROM attribs WHERE id=:dst-attrib-id),
             (SELECT title FROM attribs WHERE id=:dst-attrib-id),
       :excl-low-count-cells,
       :counts-table,
       :ip::inet,
       now());
