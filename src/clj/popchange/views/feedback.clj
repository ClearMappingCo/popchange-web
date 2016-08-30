(ns popchange.views.feedback
  (:require [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defn main-panel
  [params]
  [:div.user-login-wrapper
   [:div.user-login
    [:div.container
     [:div.user-login-form-wrapper.col-md-8.col-md-offset-2
      [:div.user-login-logo.col-md-6.col-md-offset-3
       [:img {:src "/img/popchange_logo.png" :width "100%"}]]
      [:div.user-login-form.col-md-8.col-md-offset-2
       [:div.col-md-12
        [:form.form-horizontal {:method "post" :action "/feedback"}

         (if (-> params :messages)
           [:div.form-group
            (map
             (fn [message]
               [:div.message.col-md-12 message])
             (-> params :messages))])
         
         [:div.form-group
          [:div.col-md-12
           [:textarea.form-control {:name "comments"
                                    :placeholder "Comments"
                                    :autofocus "autofocus"
                                    :rows 10}]]]
         [:div.form-group
          [:div.checkbox.col-md-12
           [:label
            [:input
             {:type "checkbox"
              :name "copyme"
              :id "copyme"}]
            "Send a copy to myself"]]]
         [:div.form-group
          [:div.col-md-12
           [:input.btn {:type "submit"
                        :value "Send feedback"}]]]
         (anti-forgery-field)]]]]]]])
