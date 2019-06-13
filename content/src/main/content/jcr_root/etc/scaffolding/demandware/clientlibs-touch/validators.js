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


/* ==========================================================================================
 * jQuery-based validators (Touch-optimized UI)
 * ==========================================================================================
 */
(function(document, $, Granite) {
    "use strict";

    /*
     * Prices
     */
    $.validator.register({
        selector: "form [data-validation='geometrixx.price']",
        validate: function(el) {
            var v = el.val();

            if (v.length > 0 && (!v.match(/^[\d\.]+$/) || isNaN(parseFloat(v)))) {
                return Granite.I18n.get("Must be a valid price.");
            }
        }
    });

    /*
     * Geometrixx-Outdoors SKUs
     */
    $.validator.register({
        selector: "form [data-validation='geometrixx.sku']",
        validate: function(el) {
            var v = el.val();

            if (v.length < 6) {
                return Granite.I18n.get("Geometrixx Outdoors SKUs must be at least 6 characters.");
            }
        }
    });

    /*
     * Geometrixx-Outdoors Currencies
     */
    $.validator.register({
        selector: "form [data-validation='geometrixx.currencyCode']",
        validate: function(el) {
            var v = el.val();

            if (!v.match(/^(USD)|(EUR)|(GBP)|(CHF)|(JPY)$/)) {
                return Granite.I18n.get("Geometrixx Outdoors supports only USD, EUR, GBP, CHF and JPY")
            }
        }
    })

})(document, Granite.$, Granite);
