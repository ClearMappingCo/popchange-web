(ns popchange.views.user-registration
  (:require [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn main-panel
  [params]
  [:div.container
   [:div.page-header
    [:h1 "User Registration"]]
   [:p.lead "Use the form below to register for a free account for full access to the web resource."]
   [:form.form-horizontal.user-registration {:method "post" :action "/register"}

    
    [:div.form-group
     [:label.col-md-3.control-label {:for "username"}
      "Username"]
     [:div.col-md-9
      [:input.form-control {:type "text"
                            :name "username"
                            :id "username"
                            :value (:username params)}]]

     (if (-> params :errors :username)
       [:div.error.col-md-9.col-md-offset-3 (-> params :errors :username)])]
    
    [:div.form-group
     [:label.col-md-3.control-label {:for "name"}
      "Name"]
     [:div.col-md-9
      [:input.form-control {:type "text"
                            :name "name"
                            :id "name"
                            :value (:name params)}]]
     
     (if (-> params :errors :name)
       [:div.error.col-md-9.col-md-offset-3 (-> params :errors :name)])]
    
    [:div.form-group
     [:label.col-md-3.control-label {:for "email"}
      "Email"]
     [:div.col-md-9
      [:input.form-control {:type "text"
                            :name "email"
                            :id "email"
                            :value (:email params)}]]
     
     (if (-> params :errors :email)
       [:div.error.col-md-9.col-md-offset-3 (-> params :errors :email)])]
    
    [:div.form-group
     [:label.col-md-3.control-label {:for "password"}
      "Password"]
     [:div.col-md-9
      [:input.form-control {:type "password"
                            :name "password"
                            :id "password"}]]
     
     (if (-> params :errors :password)
       [:div.error.col-md-9.col-md-offset-3 (-> params :errors :password)])]
    
    [:div.form-group
     [:label.col-md-3.control-label {:for "password-confirm"}
      "Confirm Password"]
     [:div.col-md-9
      [:input.form-control {:type "password"
                            :name "password-confirm"
                            :id "password-confirm"}]]
     
     (if (-> params :errors :password-confirm)
       [:div.error.col-md-9.col-md-offset-3 (-> params :errors :password-confirm)])]
    
    [:div.form-group
     [:label.col-md-3.control-label {:for "sector"}
      "Sector"]
     [:div.col-md-9
      [:select.form-control {:name "sector"
                             :id "sector"}
       (map
        (fn [sector]
          (let [selected (if (= (:id sector) (:sector params)) {:selected "selected"})]
            [:option
             (merge selected {:value (:id sector)})
             (:title sector)]))
        (:sectors params))]]
     
     (if (-> params :errors :sector)
       [:div.error.col-md-9.col-md-offset-3 (-> params :errors :sector)])]
    
    [:div.form-group
     [:div.checkbox.col-md-offset-3.col-md-9
      [:label
       [:input
        (let [ml-checked (if (:mailinglist params) {:checked "checked"})]
          (merge
           ml-checked
           {:type "checkbox"
            :name "mailinglist"
            :id "mailinglist"}))]
       "Join mailing list"]]]
    [:div.form-group
     [:div.col-md-offset-3.col-md-9
      [:input.btn.btn-success {:type "submit"
                                      :value "Register"}]]]
    (anti-forgery-field)]])
