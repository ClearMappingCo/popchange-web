(ns popchange.views.user-password-reset-confirm
  (:require [ring.util.anti-forgery :refer [anti-forgery-field]]))

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
        [:form.form-horizontal {:method "post" :action "/password-reset-confirm"}

         [:div.form-group
            [:div.col-md-12
             [:input.form-control {:name "password"
                                   :type "password"
                                   :placeholder "New password"
                                   :autofocus "autofocus"}]]]
         [:div.form-group
          [:div.col-md-12
           [:input.form-control {:name "password-confirm"
                                 :type "password"
                                 :placeholder "Confirm new password"}]]]

         (if (-> params :errors)
           [:div.form-group
            (map
             (fn [error]
               [:div.error.col-md-12 error])
             (-> params :errors))])

         (if (-> params :messages)
           [:div.form-group
            (map
             (fn [message]
               [:div.message.col-md-12 message])
             (-> params :messages))])
         
         [:div.form-group
          [:div.col-md-12
           [:input.btn {:name "pwreset"
                        :type "submit"
                        :value "Reset password"}]]]
         [:input {:name "token"
                  :type "hidden"
                  :value (:t params)}]
         (anti-forgery-field)]]]]]]
   [:div.user-login-intro
    [:div.container
     [:div.col-md-10.col-md-offset-1.text-center
      [:p.lead "Welcome to our visualisation tool for Census data."]
      [:p.lead "Compare any Census population data, such as deprivation, ethnicity or health, for years between 1971 to 2011, on a map of your local area."]
      [:p.lead "This is a FREE tool, but please register above to utilise fully."]]]]])
