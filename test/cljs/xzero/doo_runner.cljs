(ns xzero.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [xzero.core-test]))

(doo-tests 'xzero.core-test)

