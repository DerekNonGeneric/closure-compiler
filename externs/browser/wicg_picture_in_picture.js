/*
 * Copyright 2021 The Closure Compiler Authors
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
 */

/**
 * @fileoverview Picture-in-picture APIs.
 * @see https://wicg.github.io/picture-in-picture/
 * @externs
 */

/**
 * @record
 * @extends {EventInit}
 */
var PictureInPictureEventInit;

/** @type {!PictureInPictureWindow} */
PictureInPictureEventInit.prototype.pictureInPictureWindow;

/**
 * @constructor
 * @extends {Event}
 * @param {string} type
 * @param {!PictureInPictureEventInit} eventInitDict
 * @see https://developer.mozilla.org/docs/Web/API/PictureInPictureEvent
 */
function PictureInPictureEvent(type, eventInitDict) {}

/** @type {!PictureInPictureWindow} */
PictureInPictureEvent.prototype.pictureInPictureWindow;


/**
 * @interface
 * @extends {EventTarget}
 * @see https://wicg.github.io/picture-in-picture/#interface-picture-in-picture-window
 */
function PictureInPictureWindow() {}

/** @type {number} */
PictureInPictureWindow.prototype.width;

/** @type {number} */
PictureInPictureWindow.prototype.height;

/** @type {?function(!Event)} */
PictureInPictureWindow.prototype.onresize;

/**
 * @see https://wicg.github.io/picture-in-picture/#htmlvideoelement-extensions
 * @return {!Promise<!PictureInPictureWindow>}
 */
HTMLVideoElement.prototype.requestPictureInPicture = function() {};

/**
 * @type {?function(!Event)}
 * @see https://wicg.github.io/picture-in-picture/#htmlvideoelement-extensions
 */
HTMLVideoElement.prototype.onenterpictureinpicture;

/**
 * @type {?function(!Event)}
 * @see https://wicg.github.io/picture-in-picture/#htmlvideoelement-extensions
 */
HTMLVideoElement.prototype.onleavepictureinpicture;

/**
 * @type {boolean}
 * @see https://wicg.github.io/picture-in-picture/#htmlvideoelement-extensions
 */
HTMLVideoElement.prototype.autoPictureInPicture;

/**
 * @type {boolean}
 * @see https://wicg.github.io/picture-in-picture/#htmlvideoelement-extensions
 */
HTMLVideoElement.prototype.disablePictureInPicture;

/**
 * @type {boolean}
 * @see https://wicg.github.io/picture-in-picture/#document-extensions
 */
Document.prototype.pictureInPictureEnabled;

/**
 * @see https://wicg.github.io/picture-in-picture/#document-extensions
 * @return {!Promise<void>}
 */
Document.prototype.exitPictureInPicture = function() {};

/**
 * @type {?Element}
 * @see https://wicg.github.io/picture-in-picture/#documentorshadowroot-extension
 */
Document.prototype.pictureInPictureElement;
