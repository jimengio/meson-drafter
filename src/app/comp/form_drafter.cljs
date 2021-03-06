
(ns app.comp.form-drafter
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.core
             :refer
             [defcomp defeffect <> >> list-> div button textarea span input a]]
            [respo.comp.space :refer [=<]]
            [reel.comp.reel :refer [comp-reel]]
            [respo-md.comp.md :refer [comp-md]]
            [app.config :refer [dev?]]
            [respo-ui.comp :refer [comp-tabs comp-placeholder]]
            [respo-alerts.core :refer [use-modal-menu use-prompt]]
            [cljs.reader :refer [read-string]]
            [feather.core :refer [comp-icon]]
            [medley.core :refer [find-first]]))

(def field-types
  [{:value :input, :display "Input"}
   {:value :number, :display "Number"}
   {:value :textarea, :display "Textarea"}
   {:value :select, :display "Select"}
   {:value :decorative, :display "Decorative"}
   {:value :custom, :display "Custom"}
   {:value :group, :display "Group"}
   {:value :nested, :display "Nested"}])

(defn render-field-type [kind]
  (let [target (find-first (fn [info] (= kind (:value info))) field-types)]
    (if (some? target) (:display target) (str kind))))

(defcomp
 comp-field-info
 (states field path)
 (let [cursor (:cursor states)
       state (or (:data states) {:dragover? false})
       create-menu (use-modal-menu
                    (>> states :create)
                    {:title "Create field",
                     :style {},
                     :items field-types,
                     :on-result (fn [result d!]
                       (d! :create-field {:type (:value result), :path path}))})
       edit-label (use-prompt (>> states :label) {:title "Edit label"})
       has-children? (or (= :group (:type field)) (= :nested (:type field)))]
   (div
    {:style (merge
             ui/column
             {:background-color (hsl 0 0 98),
              :padding "2px 8px",
              :margin-bottom 8,
              :border (str "1px solid " (hsl 0 0 90))}
             (if (:dragover? state) {:outline (str "2px solid " (hsl 200 80 70))})),
     :draggable true,
     :on-dragstart (fn [e d!]
       (-> (:event e) .-dataTransfer (.setData "path" (pr-str path)))),
     :on-dragover (fn [e d!] (.preventDefault (:event e))),
     :on-drop (fn [e d!]
       (let [from (read-string (-> (:event e) .-dataTransfer (.getData "path")))]
         (d! :drag-field {:from from, :to path})
         (d! cursor (assoc state :dragover? false)))),
     :on-dragenter (fn [e d!] (d! cursor (assoc state :dragover? true))),
     :on-dragleave (fn [e d!]
       (when-not (-> e :event .-currentTarget (.contains (-> e :event .-relatedTarget)))
         (d! cursor (assoc state :dragover? false))))}
    (div
     {:style ui/row-parted}
     (<> (render-field-type (:type field)) {:font-family ui/font-fancy, :font-size 16})
     (div
      {}
      (if-not has-children?
        (input
         {:type "checkbox",
          :style {:cursor :pointer, :vertical-align :middle},
          :required (:required? field),
          :on-click (fn [e d!] (d! :toggle-required path))}))
      (=< 8 nil)
      (if-not (= :group (:type field))
        (span
         {:inner-text (or (:label field) "TODO"),
          :style {:cursor :pointer},
          :on-click (fn [e d!]
            ((:show edit-label) d! (fn [text] (d! :update-label {:path path, :text text}))))})))
     (comp-icon
      :x
      {:font-size 14, :color (hsl 0 80 70), :cursor :pointer}
      (fn [e d!] (d! :remove-field path))))
    (if has-children?
      (div
       {:style {:margin-left 16}}
       (list->
        {}
        (->> (:children field)
             (map-indexed
              (fn [idx child] [idx (comp-field-info (>> states idx) child (conj path idx))]))))
       (div
        {:style (merge ui/center {:padding 8})}
        (a
         {:style (merge ui/link {:line-height "14px", :height "14px"}),
          :inner-text "Add",
          :on-click (fn [e d!] ((:show create-menu) d!)),
          :on-dragover (fn [e d!] (.preventDefault (:event e))),
          :on-drop (fn [e d!]
            (let [from (read-string (-> (:event e) .-dataTransfer (.getData "path")))]
              (d! :drag-field {:from from, :to (conj path (count (:children field)))})))}))))
    (:ui create-menu)
    (:ui edit-label))))

(defcomp
 comp-form-drafter
 (states fields styles)
 (let [create-menu (use-modal-menu
                    (>> states :create)
                    {:title "Create field",
                     :style {},
                     :items field-types,
                     :on-result (fn [result d!]
                       (d! :create-field {:type (:value result), :path []}))})]
   (div
    {:style (merge ui/expand styles {:padding 16})}
    (div
     {}
     (list->
      {}
      (->> fields
           (map-indexed
            (fn [idx field] [idx (comp-field-info (>> states idx) field [idx])]))))
     (div
      {:style (merge ui/center {:padding 8}), :onc nil}
      (a
       {:inner-text "Add", :style ui/link, :on-click (fn [e d!] ((:show create-menu) d!))})))
    (:ui create-menu))))
