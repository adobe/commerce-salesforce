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
// Date format helper script

"use strict";

use(function () {
    var formattedDate = "";
    var formattedWeekDay = "";

    if ((this.pattern !== undefined) && (this.pattern !== null) && (this.date !== undefined) && (this.date !== null)) {
        formattedDate = new java.text.SimpleDateFormat(this.pattern).format(this.date);
    }


    String.prototype.capitalizeFirstLetter = function () {
        return this.charAt(0).toUpperCase() + this.slice(1);
    }

    if ((this.weekday !== undefined) && (this.weekday !== null)) {
        formattedWeekDay = this.weekday.capitalizeFirstLetter();
    }

    return {
        formattedDate: formattedDate,
        formattedWeekDay: formattedWeekDay
    };
});