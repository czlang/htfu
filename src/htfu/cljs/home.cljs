(ns htfu.cljs.home
  (:require-macros
   [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [cljs.pprint :refer [pprint]]
            [htfu.cljs.common :refer [debug]]
            [re-frame.core :as re-frame]
            [htfu.cljc.cljc-util :as cljc-util]
            cljsjs.material-ui
            [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.reagent :as rui]
            [cljs-react-material-ui.icons :as ic]))



(defn item-form []
  (fn[]
    [rui/paper {:zDepth 2 :style {:margin "10px"
                                  :padding "5px 10px 10px 10px"}}
     [:h4 "New Item"]
     [rui/text-field {:floatingLabelText "Title"
                      :underlineShow false}]
     [rui/divider]
     [rui/text-field {:floatingLabelText "Description"
                      :underlineShow false
                      :multiLine true
                      :fullWidth true}]
     [rui/divider]
     [rui/raised-button {:label        "Save item"
                         :icon         (ic/content-save)
                         :on-touch-tap #(prn "SAVE ITEM")
                         :style {:margin "10px 0 0 0"}}]]))

(defn set-form []
  (fn[]
    [rui/paper {:zDepth 2 :style {:margin "10px"
                                  :padding "5px 10px 10px 10px"}}
     [:h4 "New Set"]
     [rui/text-field {:floatingLabelText "Title"
                      :underlineShow false}]
     [rui/divider]
     [rui/text-field {:floatingLabelText "Description"
                      :underlineShow false
                      :multiLine true
                      :fullWidth true}]
     [rui/divider]
     [rui/raised-button {:label        "Save set"
                         :icon         (ic/content-save)
                         :on-touch-tap #(prn "SAVE SET")
                         :style {:margin "10px 0 0 0"}}]]))

(defn home []
  (let [testing (reagent/atom 0)
        show-new-item? (reagent/atom false)
        show-new-set? (reagent/atom false)]
    (fn []
      [rui/mui-theme-provider
       [rui/paper
        [rui/app-bar {:title "HTFU!"
                      :onLeftIconButtonTouchTap #(reset! testing (inc @testing))}]
        [rui/raised-button {:label        "New item"
                            :icon         (ic/content-add)
                            :on-touch-tap #(reset! show-new-item? (not @show-new-item?))}]
        [rui/raised-button {:label        "New set"
                            :icon         (ic/content-add-circle)
                            :on-touch-tap #(reset! show-new-set? (not @show-new-set?))}]

        

        (when @show-new-item?
          [item-form])
        (when @show-new-set?
          [set-form])]])))





