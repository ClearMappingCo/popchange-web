(ns popchange.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [popchange.core-test]))

(doo-tests 'popchange.core-test)

