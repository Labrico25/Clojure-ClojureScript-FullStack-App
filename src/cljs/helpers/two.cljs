(ns helpers.two)


(set! *warn-on-infer* true)

(defn mapify [iter]
  (loop [acc {}]
    (let [elem (.next iter)]
      (if (. elem -done)
        acc
        (let [[k v] (array-seq (. elem -value))]
          (recur (assoc acc k v)))))))

(defn base-url [path]
  (str js/window.location.origin path))