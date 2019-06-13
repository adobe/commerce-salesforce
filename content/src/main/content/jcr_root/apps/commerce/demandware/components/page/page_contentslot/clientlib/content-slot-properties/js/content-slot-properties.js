/*******************************************************************************
 * Copyright 2018 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
(function (document, Granite, $) {
    "use strict";


    $(document).on("click", ".cq-siteadmin-properties-demandware-selectslotcontenttype .coral-SelectList-item", function () {
        var $this = $(this);
        var $parentConatiner = $this.closest(".coral-FixedColumn").find(".cq-siteadmin-properties-demandware-slotcontenttypes");
        var selection = $this.attr("data-value");

        switchContenType(selection, $parentConatiner);
    });

    var $selector = $(".cq-siteadmin-properties-demandware-selectslotcontenttype")[0];

    $($selector).ready(function () {
        var $selectslottype = $(".cq-siteadmin-properties-demandware-selectslotcontenttype .coral-Select-select option:selected");
        var selection = $selectslottype.val();
        var $parentConatiner = $selectslottype.closest(".coral-FixedColumn").find(".cq-siteadmin-properties-demandware-slotcontenttypes");

        switchContenType(selection, $parentConatiner);

        $(document).on("click", "#shell-propertiespage-saveactivator", function () {
            var selection = $(".cq-siteadmin-properties-demandware-selectslotcontenttype select").val();
            if (selection === "products") {
                $("div.cq-siteadmin-properties-demandware-slotcontenttype-category :input[type=text]").remove();
                $("div.cq-siteadmin-properties-deﬂmandware-slotcontenttype-contentasset :input[type=text]").remove();
                $("textarea.cq-siteadmin-properties-demandware-slotcontenttype-html").val("");
            } else if (selection === "categories") {
                $("div.cq-siteadmin-properties-demandware-slotcontenttype-product :input[type=text]").remove();
                $("div.cq-siteadmin-properties-demandware-slotcontenttype-contentasset :input[type=text]").remove();
                $("textarea.cq-siteadmin-properties-demandware-slotcontenttype-html").val("");

            } else if (selection === "content_assets") {
                $("div.cq-siteadmin-properties-demandware-slotcontenttype-product :input[type=text]").remove();
                $("div.cq-siteadmin-properties-demandware-slotcontenttype-category :input[type=text]").remove();
                $("textarea.cq-siteadmin-properties-demandware-slotcontenttype-html").val("");

            } else if (selection === "html") {
                $("div.cq-siteadmin-properties-demandware-slotcontenttype-product :input[type=text]").remove();
                $("div.cq-siteadmin-properties-demandware-slotcontenttype-category :input[type=text]").remove();
                $("div.cq-siteadmin-properties-demandware-slotcontenttype-contentasset :input[type=text]").remove();
            }
        });
    });

    function switchContenType(contenType, uiConatiner) {
        var $productContentField = uiConatiner.find(".cq-siteadmin-properties-demandware-slotcontenttype-product").closest(".coral-Form-fieldwrapper");
        var $categoryContentField = uiConatiner.find(".cq-siteadmin-properties-demandware-slotcontenttype-category").closest(".coral-Form-fieldwrapper");
        var $contentAssetContentField = uiConatiner.find(".cq-siteadmin-properties-demandware-slotcontenttype-contentasset").closest(".coral-Form-fieldwrapper");
        var $htmlContentField = uiConatiner.find(".cq-siteadmin-properties-demandware-slotcontenttype-html").closest(".coral-Form-fieldwrapper");


        $productContentField.attr("hidden", true);
        $categoryContentField.attr("hidden", true);
        $contentAssetContentField.attr("hidden", true);
        $htmlContentField.attr("hidden", true);

        if (contenType === "products") {
            $productContentField.removeAttr("hidden");
        } else if (contenType === "categories") {
            $categoryContentField.removeAttr("hidden");
        } else if (contenType === "content_assets") {
            $contentAssetContentField.removeAttr("hidden");
        } else if (contenType === "html") {
            $htmlContentField.removeAttr("hidden");
        }
    }

})(document, Granite, Granite.$);