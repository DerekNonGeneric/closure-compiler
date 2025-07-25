/*
 * Copyright 2008 The Closure Compiler Authors
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
 * @fileoverview Definitions for all the extensions over the
 *  W3C's DOM3 specification in HTML5. This file depends on
 *  w3c_dom3.js. The whole file has been fully type annotated.
 *
 *  @see http://www.whatwg.org/specs/web-apps/current-work/multipage/index.html
 *  @see http://dev.w3.org/html5/spec/Overview.html
 *
 *  This also includes Typed Array definitions from
 *  http://www.khronos.org/registry/typedarray/specs/latest/
 *
 *  This relies on w3c_event.js being included first.
 *
 * @externs
 */

/** @type {?HTMLSlotElement} */
Node.prototype.assignedSlot;

/**
 * @type {string}
 * @see https://dom.spec.whatwg.org/#dom-element-slot
 */
Element.prototype.slot;

/**
 * Note: In IE, the contains() method only exists on Elements, not Nodes.
 * Therefore, it is recommended that you use the Conformance framework to
 * prevent calling this on Nodes which are not Elements.
 * @see https://connect.microsoft.com/IE/feedback/details/780874/node-contains-is-incorrect
 *
 * @param {Node} n The node to check
 * @return {boolean} If 'n' is this Node, or is contained within this Node.
 * @see https://developer.mozilla.org/en-US/docs/Web/API/Node.contains
 * @nosideeffects
 */
Node.prototype.contains = function(n) {};

/** @type {boolean} */
Node.prototype.isConnected;

/**
 * @type {boolean}
 * @see https://html.spec.whatwg.org/multipage/scripting.html#the-script-element
 */
HTMLScriptElement.prototype.async;

/**
 * @type {string?}
 * @see https://html.spec.whatwg.org/multipage/scripting.html#the-script-element
 */
HTMLScriptElement.prototype.crossOrigin;

/**
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/scripting.html#the-script-element
 */
HTMLScriptElement.prototype.integrity;

/**
 * @type {boolean}
 * @see https://html.spec.whatwg.org/multipage/scripting.html#the-script-element
 */
HTMLScriptElement.prototype.noModule;

/**
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/scripting.html#the-script-element
 */
HTMLScriptElement.prototype.referrerPolicy;

/**
 * @constructor
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/the-canvas-element.html#the-canvas-element
 * @extends {HTMLElement}
 */
function HTMLCanvasElement() {}

/** @type {number} */
HTMLCanvasElement.prototype.width;

/** @type {number} */
HTMLCanvasElement.prototype.height;

/**
 * @see https://www.w3.org/TR/html5/scripting-1.html#dom-canvas-toblob
 * @param {function(!Blob)} callback
 * @param {string=} opt_type
 * @param {...*} var_args
 * @throws {Error}
 */
HTMLCanvasElement.prototype.toBlob = function(callback, opt_type, var_args) {};

/**
 * @param {string=} opt_type
 * @param {...*} var_args
 * @return {string}
 * @throws {Error}
 */
HTMLCanvasElement.prototype.toDataURL = function(opt_type, var_args) {};

/**
 * @modifies {this}
 * @param {string} contextId
 * @param {Object=} opt_args
 * @return {Object}
 */
HTMLCanvasElement.prototype.getContext = function(contextId, opt_args) {};

/**
 * @see https://www.w3.org/TR/mediacapture-fromelement/
 * @param {number=} opt_framerate
 * @return {!MediaStream}
 * @throws {Error}
 */
HTMLCanvasElement.prototype.captureStream = function(opt_framerate) {};

/**
 * @see https://html.spec.whatwg.org/multipage/canvas.html#dom-canvas-transfercontroltooffscreen
 * @return {!OffscreenCanvas}
 * @throws {Error}
 */
HTMLCanvasElement.prototype.transferControlToOffscreen = function() {};

/**
 * @constructor
 * @extends {MediaStreamTrack}
 * @see https://w3c.github.io/mediacapture-fromelement/#the-canvascapturemediastreamtrack
 */
function CanvasCaptureMediaStreamTrack() {}

/**
 * The canvas element that this media stream captures.
 * @type {!HTMLCanvasElement}
 */
CanvasCaptureMediaStreamTrack.prototype.canvas;

/**
 * Allows applications to manually request that a frame from the canvas be
 * captured and rendered into the track.
 * @return {undefined}
 */
CanvasCaptureMediaStreamTrack.prototype.requestFrame = function() {};


/**
 * @see https://html.spec.whatwg.org/multipage/canvas.html#the-offscreencanvas-interface
 * @implements {EventTarget}
 * @param {number} width
 * @param {number} height
 * @nosideeffects
 * @constructor
 */
function OffscreenCanvas(width, height) {}

/** @override */
OffscreenCanvas.prototype.addEventListener = function(
    type, listener, opt_options) {};

/** @override */
OffscreenCanvas.prototype.removeEventListener = function(
    type, listener, opt_options) {};

/** @override */
OffscreenCanvas.prototype.dispatchEvent = function(evt) {};

/** @type {number} */
OffscreenCanvas.prototype.width;

/** @type {number} */
OffscreenCanvas.prototype.height;

/**
 * @param {string} contextId
 * @param {!Object=} opt_options
 * @modifies {this}
 * @return {!Object}
 */
OffscreenCanvas.prototype.getContext = function(contextId, opt_options) {};

/**
 * @return {!ImageBitmap}
 */
OffscreenCanvas.prototype.transferToImageBitmap = function() {};

/**
 * @param {{type: (string|undefined), quality: (number|undefined)}=} opt_options
 * @return {!Promise<!Blob>}
 */
OffscreenCanvas.prototype.convertToBlob = function(opt_options) {};

// TODO(tjgq): Find a way to add SVGImageElement to this typedef without making
// svg.js part of core.
/**
 * @typedef {HTMLImageElement|HTMLVideoElement|HTMLCanvasElement|ImageBitmap|
 *     OffscreenCanvas}
 */
var CanvasImageSource;

/**
 * @interface
 * @see https://www.w3.org/TR/2dcontext/#canvaspathmethods
 */
function CanvasPathMethods() {}

/**
 * @return {undefined}
 */
CanvasPathMethods.prototype.closePath = function() {};

/**
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 */
CanvasPathMethods.prototype.moveTo = function(x, y) {};

/**
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 */
CanvasPathMethods.prototype.lineTo = function(x, y) {};

/**
 * @param {number} cpx
 * @param {number} cpy
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 */
CanvasPathMethods.prototype.quadraticCurveTo = function(cpx, cpy, x, y) {};

/**
 * @param {number} cp1x
 * @param {number} cp1y
 * @param {number} cp2x
 * @param {number} cp2y
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 */
CanvasPathMethods.prototype.bezierCurveTo = function(
    cp1x, cp1y, cp2x, cp2y, x, y) {};

/**
 * @param {number} x1
 * @param {number} y1
 * @param {number} x2
 * @param {number} y2
 * @param {number} radius
 * @return {undefined}
 */
CanvasPathMethods.prototype.arcTo = function(x1, y1, x2, y2, radius) {};

/**
 * @param {number} x
 * @param {number} y
 * @param {number} w
 * @param {number} h
 * @return {undefined}
 */
CanvasPathMethods.prototype.rect = function(x, y, w, h) {};

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D/roundRect
 * @param {number} x
 * @param {number} y
 * @param {number} w
 * @param {number} h
 * @param {(number|!DOMPointInit|!Array<number|!DOMPointInit>)=} radii
 * @return {undefined}
 */
CanvasPathMethods.prototype.roundRect = function(x, y, w, h, radii) {};

/**
 * @param {number} x
 * @param {number} y
 * @param {number} radius
 * @param {number} startAngle
 * @param {number} endAngle
 * @param {boolean=} opt_anticlockwise
 * @return {undefined}
 */
CanvasPathMethods.prototype.arc = function(
    x, y, radius, startAngle, endAngle, opt_anticlockwise) {};

/**
 * @constructor
 * @param {!Path2D|string=} arg
 * @implements {CanvasPathMethods}
 * @see https://html.spec.whatwg.org/multipage/scripting.html#path2d-objects
 */
function Path2D(arg) {}

/**
 * @return {undefined}
 * @override
 */
Path2D.prototype.closePath = function() {};

/**
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 * @override
 */
Path2D.prototype.moveTo = function(x, y) {};

/**
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 * @override
 */
Path2D.prototype.lineTo = function(x, y) {};

/**
 * @param {number} cpx
 * @param {number} cpy
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 * @override
 */
Path2D.prototype.quadraticCurveTo = function(cpx, cpy, x, y) {};

/**
 * @param {number} cp1x
 * @param {number} cp1y
 * @param {number} cp2x
 * @param {number} cp2y
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 * @override
 */
Path2D.prototype.bezierCurveTo = function(cp1x, cp1y, cp2x, cp2y, x, y) {};

/**
 * @param {number} x1
 * @param {number} y1
 * @param {number} x2
 * @param {number} y2
 * @param {number} radius
 * @return {undefined}
 * @override
 */
Path2D.prototype.arcTo = function(x1, y1, x2, y2, radius) {};

/**
 * @param {number} x
 * @param {number} y
 * @param {number} w
 * @param {number} h
 * @return {undefined}
 * @override
 */
Path2D.prototype.rect = function(x, y, w, h) {};

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D/roundRect
 * @param {number} x
 * @param {number} y
 * @param {number} w
 * @param {number} h
 * @param {(number|!DOMPointInit|!Array<number|!DOMPointInit>)=} radii
 * @return {undefined}
 * @override
 */
Path2D.prototype.roundRect = function(x, y, w, h, radii) {};

/**
 * @param {number} x
 * @param {number} y
 * @param {number} radius
 * @param {number} startAngle
 * @param {number} endAngle
 * @param {boolean=} optAnticlockwise
 * @return {undefined}
 * @override
 */
Path2D.prototype.arc = function(
    x, y, radius, startAngle, endAngle, optAnticlockwise) {};

/**
 * @param {Path2D} path
 * @return {undefined}
 */
Path2D.prototype.addPath = function(path) {};

/**
 * @interface
 * @see https://www.w3.org/TR/2dcontext/#canvasdrawingstyles
 */
function CanvasDrawingStyles() {}

/** @type {number} */
CanvasDrawingStyles.prototype.lineWidth;

/** @type {string} */
CanvasDrawingStyles.prototype.lineCap;

/** @type {string} */
CanvasDrawingStyles.prototype.lineJoin;

/** @type {number} */
CanvasDrawingStyles.prototype.miterLimit;

/**
 * @param {Array<number>} segments
 * @return {undefined}
 */
CanvasDrawingStyles.prototype.setLineDash = function(segments) {};

/**
 * @return {!Array<number>}
 */
CanvasDrawingStyles.prototype.getLineDash = function() {};

/** @type {string} */
CanvasDrawingStyles.prototype.font;

/** @type {string} */
CanvasDrawingStyles.prototype.textAlign;

/** @type {string} */
CanvasDrawingStyles.prototype.textBaseline;

/** @type {string} */
CanvasDrawingStyles.prototype.letterSpacing;

/**
 * @constructor
 * @see https://html.spec.whatwg.org/multipage/canvas.html#canvasrenderingcontext2dsettings
 */
function CanvasRenderingContext2DSettings() {}

/**
 * @type {boolean}
 */
CanvasRenderingContext2DSettings.prototype.alpha;

/**
 * @type {boolean}
 */
CanvasRenderingContext2DSettings.prototype.desynchronized;

/**
 * @type {string}
 */
CanvasRenderingContext2DSettings.prototype.colorSpace;

/**
 * @type {boolean}
 */
CanvasRenderingContext2DSettings.prototype.willReadFrequently;


// TODO(dramaix): replace this with @record.
/**
 * @constructor
 * @abstract
 * @implements {CanvasDrawingStyles}
 * @implements {CanvasPathMethods}
 * @see http://www.w3.org/TR/2dcontext/#canvasrenderingcontext2d
 */
function BaseRenderingContext2D() {}

/** @const {!HTMLCanvasElement|!OffscreenCanvas} */
BaseRenderingContext2D.prototype.canvas;

/** @return {!CanvasRenderingContext2DSettings} */
BaseRenderingContext2D.prototype.getContextAttributes = function() {};

/**
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.save = function() {};

/**
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.restore = function() {};

/**
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.scale = function(x, y) {};

/**
 * @param {number} angle
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.rotate = function(angle) {};

/**
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.translate = function(x, y) {};

/**
 * @param {number} m11
 * @param {number} m12
 * @param {number} m21
 * @param {number} m22
 * @param {number} dx
 * @param {number} dy
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.transform = function(
    m11, m12, m21, m22, dx, dy) {};

/**
 * @see https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-settransform-dev
 * @param {(number|DOMMatrixReadOnly)} m11OrMatrix
 * @param {number=} m12
 * @param {number=} m21
 * @param {number=} m22
 * @param {number=} dx
 * @param {number=} dy
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.setTransform = function(
    m11OrMatrix, m12, m21, m22, dx, dy) {};

/**
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.resetTransform = function() {};

/**
 * @return {!DOMMatrixReadOnly}
 */
BaseRenderingContext2D.prototype.getTransform = function() {};

/**
 * @param {number} x0
 * @param {number} y0
 * @param {number} x1
 * @param {number} y1
 * @return {!CanvasGradient}
 * @throws {Error}
 */
BaseRenderingContext2D.prototype.createLinearGradient = function(
    x0, y0, x1, y1) {};

/**
 * @param {number} startAngle
 * @param {number} x
 * @param {number} y
 * @return {!CanvasGradient}
 * @throws {Error}
 */
BaseRenderingContext2D.prototype.createConicGradient = function(
  startAngle, x, y) {};

/**
 * @param {number} x0
 * @param {number} y0
 * @param {number} r0
 * @param {number} x1
 * @param {number} y1
 * @param {number} r1
 * @return {!CanvasGradient}
 * @throws {Error}
 */
BaseRenderingContext2D.prototype.createRadialGradient = function(
    x0, y0, r0, x1, y1, r1) {};

/**
 * @param {CanvasImageSource} image
 * @param {string} repetition
 * @return {?CanvasPattern}
 * @throws {Error}
 * @see https://html.spec.whatwg.org/multipage/scripting.html#dom-context-2d-createpattern
 */
BaseRenderingContext2D.prototype.createPattern = function(image, repetition) {};

/**
 * @param {number} x
 * @param {number} y
 * @param {number} w
 * @param {number} h
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.clearRect = function(x, y, w, h) {};

/**
 * @param {number} x
 * @param {number} y
 * @param {number} w
 * @param {number} h
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.fillRect = function(x, y, w, h) {};

/**
 * @param {number} x
 * @param {number} y
 * @param {number} w
 * @param {number} h
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.strokeRect = function(x, y, w, h) {};

/**
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.beginPath = function() {};

/**
 * @return {undefined}
 * @override
 */
BaseRenderingContext2D.prototype.closePath = function() {};

/**
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 * @override
 */
BaseRenderingContext2D.prototype.moveTo = function(x, y) {};

/**
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 * @override
 */
BaseRenderingContext2D.prototype.lineTo = function(x, y) {};

/**
 * @param {number} cpx
 * @param {number} cpy
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 * @override
 */
BaseRenderingContext2D.prototype.quadraticCurveTo = function(cpx, cpy, x, y) {};

/**
 * @param {number} cp1x
 * @param {number} cp1y
 * @param {number} cp2x
 * @param {number} cp2y
 * @param {number} x
 * @param {number} y
 * @return {undefined}
 * @override
 */
BaseRenderingContext2D.prototype.bezierCurveTo = function(
    cp1x, cp1y, cp2x, cp2y, x, y) {};

/**
 * @param {number} x1
 * @param {number} y1
 * @param {number} x2
 * @param {number} y2
 * @param {number} radius
 * @return {undefined}
 * @override
 */
BaseRenderingContext2D.prototype.arcTo = function(x1, y1, x2, y2, radius) {};

/**
 * @param {number} x
 * @param {number} y
 * @param {number} w
 * @param {number} h
 * @return {undefined}
 * @override
 */
BaseRenderingContext2D.prototype.rect = function(x, y, w, h) {};

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D/roundRect
 * @param {number} x
 * @param {number} y
 * @param {number} w
 * @param {number} h
 * @param {(number|!DOMPointInit|!Array<number|!DOMPointInit>)=} radii
 * @return {undefined}
 * @override
 */
BaseRenderingContext2D.prototype.roundRect = function(x, y, w, h, radii) {};

/**
 * @param {number} x
 * @param {number} y
 * @param {number} radius
 * @param {number} startAngle
 * @param {number} endAngle
 * @param {boolean=} opt_anticlockwise
 * @return {undefined}
 * @override
 */
BaseRenderingContext2D.prototype.arc = function(
    x, y, radius, startAngle, endAngle, opt_anticlockwise) {};

/**
 * @param {number} x
 * @param {number} y
 * @param {number} radiusX
 * @param {number} radiusY
 * @param {number} rotation
 * @param {number} startAngle
 * @param {number} endAngle
 * @param {boolean=} opt_anticlockwise
 * @return {undefined}
 * @see http://developer.mozilla.org/en/docs/Web/API/CanvasRenderingContext2D/ellipse
 */
BaseRenderingContext2D.prototype.ellipse = function(
    x, y, radiusX, radiusY, rotation, startAngle, endAngle,
    opt_anticlockwise) {};

/**
 * @param {Path2D|string=} optFillRuleOrPath
 * @param {string=} optFillRule
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.fill = function(
    optFillRuleOrPath, optFillRule) {};

/**
 * @param {Path2D=} optStroke
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.stroke = function(optStroke) {};

/**
 * @param {Element} element
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.drawFocusIfNeeded = function(element) {};

/**
 * @param {Path2D|string=} optFillRuleOrPath
 * @param {string=} optFillRule
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.clip = function(
    optFillRuleOrPath, optFillRule) {};

/**
 * @param {number} x
 * @param {number} y
 * @return {boolean}
 * @nosideeffects
 * @see http://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D/isPointInStroke
 */
BaseRenderingContext2D.prototype.isPointInStroke = function(x, y) {};

/**
 * @param {!Path2D|number} pathOrX
 * @param {number} xOrY
 * @param {number|string=} yOrFillRule
 * @param {string=} fillRule
 * @return {boolean}
 * @nosideeffects
 * @see http://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D/isPointInPath
 */
BaseRenderingContext2D.prototype.isPointInPath = function(
    pathOrX, xOrY, yOrFillRule, fillRule) {};

/**
 * @param {string} text
 * @param {number} x
 * @param {number} y
 * @param {number=} opt_maxWidth
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.fillText = function(
    text, x, y, opt_maxWidth) {};

/**
 * @param {string} text
 * @param {number} x
 * @param {number} y
 * @param {number=} opt_maxWidth
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.strokeText = function(
    text, x, y, opt_maxWidth) {};

/**
 * @param {string} text
 * @return {!TextMetrics}
 * @nosideeffects
 */
BaseRenderingContext2D.prototype.measureText = function(text) {};

/**
 * @param {CanvasImageSource} image
 * @param {number} dx Destination x coordinate.
 * @param {number} dy Destination y coordinate.
 * @param {number=} opt_dw Destination box width.  Defaults to the image width.
 * @param {number=} opt_dh Destination box height.
 *     Defaults to the image height.
 * @param {number=} opt_sx Source box x coordinate.  Used to select a portion of
 *     the source image to draw.  Defaults to 0.
 * @param {number=} opt_sy Source box y coordinate.  Used to select a portion of
 *     the source image to draw.  Defaults to 0.
 * @param {number=} opt_sw Source box width.  Used to select a portion of
 *     the source image to draw.  Defaults to the full image width.
 * @param {number=} opt_sh Source box height.  Used to select a portion of
 *     the source image to draw.  Defaults to the full image height.
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.drawImage = function(
    image, dx, dy, opt_dw, opt_dh, opt_sx, opt_sy, opt_sw, opt_sh) {};

/**
 * @param {number} sw
 * @param {number} sh
 * @return {!ImageData}
 * @throws {Error}
 * @nosideeffects
 */
BaseRenderingContext2D.prototype.createImageData = function(sw, sh) {};

/**
 * @param {number} sx
 * @param {number} sy
 * @param {number} sw
 * @param {number} sh
 * @return {!ImageData}
 * @throws {Error}
 */
BaseRenderingContext2D.prototype.getImageData = function(sx, sy, sw, sh) {};

/**
 * @param {ImageData} imagedata
 * @param {number} dx
 * @param {number} dy
 * @param {number=} opt_dirtyX
 * @param {number=} opt_dirtyY
 * @param {number=} opt_dirtyWidth
 * @param {number=} opt_dirtyHeight
 * @return {undefined}
 */
BaseRenderingContext2D.prototype.putImageData = function(
    imagedata, dx, dy, opt_dirtyX, opt_dirtyY, opt_dirtyWidth,
    opt_dirtyHeight) {};

/**
 * Note: WebKit only
 * @param {number|string=} opt_a
 * @param {number=} opt_b
 * @param {number=} opt_c
 * @param {number=} opt_d
 * @param {number=} opt_e
 * @see http://developer.apple.com/library/safari/#documentation/appleapplications/reference/WebKitDOMRef/CanvasRenderingContext2D_idl/Classes/CanvasRenderingContext2D/index.html
 * @return {undefined}
 * @deprecated
 */
BaseRenderingContext2D.prototype.setFillColor = function(
    opt_a, opt_b, opt_c, opt_d, opt_e) {};

/**
 * Note: WebKit only
 * @param {number|string=} opt_a
 * @param {number=} opt_b
 * @param {number=} opt_c
 * @param {number=} opt_d
 * @param {number=} opt_e
 * @see http://developer.apple.com/library/safari/#documentation/appleapplications/reference/WebKitDOMRef/CanvasRenderingContext2D_idl/Classes/CanvasRenderingContext2D/index.html
 * @return {undefined}
 * @deprecated
 */
BaseRenderingContext2D.prototype.setStrokeColor = function(
    opt_a, opt_b, opt_c, opt_d, opt_e) {};

/**
 * @return {!Array<number>}
 * @override
 */
BaseRenderingContext2D.prototype.getLineDash = function() {};

/**
 * @param {Array<number>} segments
 * @return {undefined}
 * @override
 */
BaseRenderingContext2D.prototype.setLineDash = function(segments) {};

/** @type {string} */
BaseRenderingContext2D.prototype.fillColor;

/**
 * @type {string|!CanvasGradient|!CanvasPattern}
 * @see https://html.spec.whatwg.org/multipage/scripting.html#fill-and-stroke-styles:dom-context-2d-fillstyle
 * @implicitCast
 */
BaseRenderingContext2D.prototype.fillStyle;

/** @type {string} */
BaseRenderingContext2D.prototype.font;

/** @type {number} */
BaseRenderingContext2D.prototype.globalAlpha;

/** @type {string} */
BaseRenderingContext2D.prototype.globalCompositeOperation;

/** @type {number} */
BaseRenderingContext2D.prototype.lineWidth;

/** @type {string} */
BaseRenderingContext2D.prototype.lineCap;

/** @type {string} */
BaseRenderingContext2D.prototype.lineJoin;

/** @type {number} */
BaseRenderingContext2D.prototype.miterLimit;

/** @type {number} */
BaseRenderingContext2D.prototype.shadowBlur;

/** @type {string} */
BaseRenderingContext2D.prototype.shadowColor;

/** @type {number} */
BaseRenderingContext2D.prototype.shadowOffsetX;

/** @type {number} */
BaseRenderingContext2D.prototype.shadowOffsetY;

/** @type {boolean} */
BaseRenderingContext2D.prototype.imageSmoothingEnabled;

/**
 * @type {string}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D/imageSmoothingQuality
 */
BaseRenderingContext2D.prototype.imageSmoothingQuality;

/**
 * @type {string|!CanvasGradient|!CanvasPattern}
 * @see https://html.spec.whatwg.org/multipage/scripting.html#fill-and-stroke-styles:dom-context-2d-strokestyle
 * @implicitCast
 */
BaseRenderingContext2D.prototype.strokeStyle;

/** @type {string} */
BaseRenderingContext2D.prototype.strokeColor;

/** @type {string} */
BaseRenderingContext2D.prototype.textAlign;

/** @type {string} */
BaseRenderingContext2D.prototype.textBaseline;

/** @type {string} */
BaseRenderingContext2D.prototype.letterSpacing;

/** @type {number} */
BaseRenderingContext2D.prototype.lineDashOffset;

/**
 * @type {string}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D/direction
 */
BaseRenderingContext2D.prototype.direction;

/**
 * @type {string}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D/filter
 */
BaseRenderingContext2D.prototype.filter;

/**
 * @constructor
 * @extends {BaseRenderingContext2D}
 * @see http://www.w3.org/TR/2dcontext/#canvasrenderingcontext2d
 */
function CanvasRenderingContext2D() {}

/** @const {!HTMLCanvasElement} */
CanvasRenderingContext2D.prototype.canvas;

/**
 * @constructor
 * @extends {BaseRenderingContext2D}
 * @see http://www.w3.org/TR/2dcontext/#canvasrenderingcontext2d
 */
function OffscreenCanvasRenderingContext2D() {}

/** @const {!OffscreenCanvas} */
OffscreenCanvasRenderingContext2D.prototype.canvas;

/**
 * @constructor
 */
function CanvasGradient() {}

/**
 * @param {number} offset
 * @param {string} color
 * @return {undefined}
 */
CanvasGradient.prototype.addColorStop = function(offset, color) {};

/**
 * @constructor
 */
function CanvasPattern() {}

/**
 * @see https://html.spec.whatwg.org/multipage/canvas.html#dom-canvaspattern-settransform-dev
 * @param {DOMMatrixReadOnly} matrix
 * @return {undefined}
 */
CanvasPattern.prototype.setTransform = function(matrix) {};

/**
 * @constructor
 * https://developer.mozilla.org/en-US/docs/Web/API/TextMetrics
 */
function TextMetrics() {}

/** @const {number} */
TextMetrics.prototype.width;

/** @const {number|undefined} */
TextMetrics.prototype.actualBoundingBoxAscent;

/** @const {number|undefined} */
TextMetrics.prototype.actualBoundingBoxDescent;

/** @const {number|undefined} */
TextMetrics.prototype.actualBoundingBoxLeft;

/** @const {number|undefined} */
TextMetrics.prototype.actualBoundingBoxRight;

/** @const {number|undefined} */
TextMetrics.prototype.fontBoundingBoxAscent;

/** @const {number|undefined} */
TextMetrics.prototype.fontBoundingBoxDescent;

/** @const {number|undefined} */
TextMetrics.prototype.emHeightAscent;

/** @const {number|undefined} */
TextMetrics.prototype.emHeightDescent;

/** @const {number|undefined} */
TextMetrics.prototype.hangingBaseline;

/** @const {number|undefined} */
TextMetrics.prototype.alphabeticBaseline;

/** @const {number|undefined} */
TextMetrics.prototype.ideographicBaseline;

/**
 * @param {!Uint8ClampedArray|number} dataOrWidth In the first form, this is the
 *     array of pixel data.  In the second form, this is the image width.
 * @param {number} widthOrHeight In the first form, this is the image width.  In
 *     the second form, this is the image height.
 * @param {number=} opt_height In the first form, this is the optional image
 *     height.  The second form omits this argument.
 * @see https://html.spec.whatwg.org/multipage/scripting.html#imagedata
 * @constructor
 */
function ImageData(dataOrWidth, widthOrHeight, opt_height) {}

/** @const {!Uint8ClampedArray} */
ImageData.prototype.data;

/** @const {number} */
ImageData.prototype.width;

/** @const {number} */
ImageData.prototype.height;

/**
 * @see https://www.w3.org/TR/html51/webappapis.html#webappapis-images
 * @interface
 */
function ImageBitmap() {}

/**
 * @const {number}
 */
ImageBitmap.prototype.width;

/**
 * @const {number}
 */
ImageBitmap.prototype.height;

/**
 * Releases ImageBitmap's underlying bitmap data.
 * @return {undefined}
 * @see https://html.spec.whatwg.org/multipage/imagebitmap-and-animations.html#images-2
 */
ImageBitmap.prototype.close = function() {};

/**
 * @typedef {!CanvasImageSource | !Blob | !ImageData}
 * @see https://html.spec.whatwg.org/multipage/imagebitmap-and-animations.html#imagebitmapsource
 */
var ImageBitmapSource;

/**
 * @typedef {{
 *   imageOrientation: (string|undefined),
 *   premultiplyAlpha: (string|undefined),
 *   colorSpaceConversion: (string|undefined),
 *   resizeWidth: (number|undefined),
 *   resizeHeight: (number|undefined),
 *   resizeQuality: (string|undefined)
 * }}
 * @see https://html.spec.whatwg.org/multipage/imagebitmap-and-animations.html#images-2
 */
var ImageBitmapOptions;

/**
 * @param {!ImageBitmapSource} image
 * @param {(number|!ImageBitmapOptions)=} sxOrOptions
 * @param {number=} sy
 * @param {number=} sw
 * @param {number=} sh
 * @param {!ImageBitmapOptions=} options
 * @return {!Promise<!ImageBitmap>}
 * @see https://html.spec.whatwg.org/multipage/imagebitmap-and-animations.html#dom-createimagebitmap
 * @see https://html.spec.whatwg.org/multipage/webappapis.html#windoworworkerglobalscope-mixin
 */
function createImageBitmap(image, sxOrOptions, sy, sw, sh, options) {}


/**
 * @constructor
 */
function ClientInformation() {}

/** @type {boolean} */
ClientInformation.prototype.onLine;

/**
 * @param {string} protocol
 * @param {string} uri
 * @param {string} title
 * @return {undefined}
 */
ClientInformation.prototype.registerProtocolHandler = function(
    protocol, uri, title) {};

/**
 * @param {string} mimeType
 * @param {string} uri
 * @param {string} title
 * @return {undefined}
 */
ClientInformation.prototype.registerContentHandler = function(
    mimeType, uri, title) {};

// HTML5 Database objects
/**
 * @constructor
 */
function Database() {}

/**
 * @type {string}
 */
Database.prototype.version;

/**
 * @param {function(!SQLTransaction) : void} callback
 * @param {(function(!SQLError) : void)=} opt_errorCallback
 * @param {Function=} opt_Callback
 * @return {undefined}
 */
Database.prototype.transaction = function(
    callback, opt_errorCallback, opt_Callback) {};

/**
 * @param {function(!SQLTransaction) : void} callback
 * @param {(function(!SQLError) : void)=} opt_errorCallback
 * @param {Function=} opt_Callback
 * @return {undefined}
 */
Database.prototype.readTransaction = function(
    callback, opt_errorCallback, opt_Callback) {};

/**
 * @param {string} oldVersion
 * @param {string} newVersion
 * @param {function(!SQLTransaction) : void} callback
 * @param {function(!SQLError) : void} errorCallback
 * @param {Function} successCallback
 * @return {undefined}
 */
Database.prototype.changeVersion = function(
    oldVersion, newVersion, callback, errorCallback, successCallback) {};

/**
 * @interface
 */
function DatabaseCallback() {}

/**
 * @param {!Database} db
 * @return {undefined}
 */
DatabaseCallback.prototype.handleEvent = function(db) {};

/**
 * @constructor
 */
function SQLError() {}

/**
 * @type {number}
 */
SQLError.prototype.code;

/**
 * @type {string}
 */
SQLError.prototype.message;

/**
 * @constructor
 */
function SQLTransaction() {}

/**
 * @param {string} sqlStatement
 * @param {Array<*>=} opt_queryArgs
 * @param {SQLStatementCallback=} opt_callback
 * @param {(function(!SQLTransaction, !SQLError) : (boolean|void))=}
 *     opt_errorCallback
 * @return {undefined}
 */
SQLTransaction.prototype.executeSql = function(
    sqlStatement, opt_queryArgs, opt_callback, opt_errorCallback) {};

/**
 * @typedef {(function(!SQLTransaction, !SQLResultSet) : void)}
 */
var SQLStatementCallback;

/**
 * @constructor
 */
function SQLResultSet() {}

/**
 * @type {number}
 */
SQLResultSet.prototype.insertId;

/**
 * @type {number}
 */
SQLResultSet.prototype.rowsAffected;

/**
 * @type {!SQLResultSetRowList}
 */
SQLResultSet.prototype.rows;

/**
 * @constructor
 * @implements {IArrayLike<!Object>}
 * @see http://www.w3.org/TR/webdatabase/#sqlresultsetrowlist
 */
function SQLResultSetRowList() {}

/**
 * @type {number}
 */
SQLResultSetRowList.prototype.length;

/**
 * @param {number} index
 * @return {Object}
 * @nosideeffects
 */
SQLResultSetRowList.prototype.item = function(index) {};

/**
 * @param {string} name
 * @param {string} version
 * @param {string} description
 * @param {number} size
 * @param {(DatabaseCallback|function(Database))=} opt_callback
 * @return {!Database}
 */
function openDatabase(name, version, description, size, opt_callback) {}

/**
 * @param {string} name
 * @param {string} version
 * @param {string} description
 * @param {number} size
 * @param {(DatabaseCallback|function(Database))=} opt_callback
 * @return {!Database}
 */
Window.prototype.openDatabase = function(
    name, version, description, size, opt_callback) {};

/**
 * @type {boolean}
 * @see https://www.w3.org/TR/html5/embedded-content-0.html#dom-img-complete
 */
HTMLImageElement.prototype.complete;

/**
 * @type {number}
 * @see https://www.w3.org/TR/html5/embedded-content-0.html#dom-img-naturalwidth
 */
HTMLImageElement.prototype.naturalWidth;

/**
 * @type {number}
 * @see https://www.w3.org/TR/html5/embedded-content-0.html#dom-img-naturalheight
 */
HTMLImageElement.prototype.naturalHeight;

/**
 * @type {string}
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/embedded-content-1.html#attr-img-crossorigin
 */
HTMLImageElement.prototype.crossOrigin;

/**
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-currentsrc
 */
HTMLImageElement.prototype.currentSrc;

/**
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/images.html#image-decoding-hint
 */
HTMLImageElement.prototype.decoding;

/**
 * @return {!Promise<undefined>}
 * @see https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-decode
 */
HTMLImageElement.prototype.decode;

/**
 * @record
 * @see https://html.spec.whatwg.org/multipage/web-messaging.html#structuredserializeoptions
 */
function StructuredSerializeOptions() {}

/**
 * @type {!Array<!Transferable>|undefined}
 * @see https://html.spec.whatwg.org/multipage/web-messaging.html#dom-structuredserializeoptions-transfer
 */
StructuredSerializeOptions.prototype.transfer;

/**
 * @record
 * @extends {StructuredSerializeOptions}
 * @see https://html.spec.whatwg.org/multipage/window-object.html#windowpostmessageoptions
 */
function WindowPostMessageOptions() {}

/**
 * @type {string|undefined}
 * @see https://html.spec.whatwg.org/multipage/window-object.html#dom-windowpostmessageoptions-targetorigin
 */
WindowPostMessageOptions.prototype.targetOrigin;

/**
 * This is a superposition of the Window and Worker postMessage methods.
 * @param {*} message
 * @param {(string|!StructuredSerializeOptions|!WindowPostMessageOptions|!Array<!Transferable>)=}
 *     targetOriginOrOptionsOrTransfer
 * @param {(string|!Array<!MessagePort>|!Array<!Transferable>)=}
 *     targetOriginOrPortsOrTransfer
 * @return {void}
 */
function postMessage(
    message, targetOriginOrOptionsOrTransfer, targetOriginOrPortsOrTransfer) {}

/**
 * Takes the input value and returns a deep copy by performing the structured
 * clone algorithm. Transferable objects listed in the transfer array are
 * transferred, not just cloned, meaning that they are no longer usable in the
 * input value.
 * @see https://html.spec.whatwg.org/multipage/structured-data.html#structured-cloning
 * @param {*} value
 * @param {!StructuredSerializeOptions=} options
 * @throws {DOMException}
 * @return {*}
 */
function structuredClone(value, options) {}

/**
 * @param {*} message
 * @param {(string|!WindowPostMessageOptions)=} targetOriginOrOptions
 * @param {(!Array<!Transferable>)=} transfer
 * @return {void}
 * @see https://html.spec.whatwg.org/multipage/web-messaging.html#posting-messages
 */
Window.prototype.postMessage = function(
    message, targetOriginOrOptions, transfer) {};

/**
 * The postMessage method (as implemented in Opera).
 * @param {string} message
 */
Document.prototype.postMessage = function(message) {};

/**
 * Document head accessor.
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/dom.html#the-head-element-0
 * @type {HTMLHeadElement}
 */
Document.prototype.head;

/**
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/dom.html#current-document-readiness
 */
Document.prototype.readyState;

/**
 * @see https://html.spec.whatwg.org/#application-cache-api
 * @constructor
 * @implements {EventTarget}
 */
function ApplicationCache() {}

/** @override */
ApplicationCache.prototype.addEventListener = function(
    type, listener, opt_options) {};

/** @override */
ApplicationCache.prototype.removeEventListener = function(
    type, listener, opt_options) {};

/** @override */
ApplicationCache.prototype.dispatchEvent = function(evt) {};

/**
 * The object isn't associated with an application cache. This can occur if the
 * update process fails and there is no previous cache to revert to, or if there
 * is no manifest file.
 * @const {number}
 */
ApplicationCache.prototype.UNCACHED;

/**
 * The object isn't associated with an application cache. This can occur if the
 * update process fails and there is no previous cache to revert to, or if there
 * is no manifest file.
 * @const {number}
 */
ApplicationCache.UNCACHED;

/**
 * The cache is idle.
 * @const {number}
 */
ApplicationCache.prototype.IDLE;

/**
 * The cache is idle.
 * @const {number}
 */
ApplicationCache.IDLE;

/**
 * The update has started but the resources are not downloaded yet - for
 * example, this can happen when the manifest file is fetched.
 * @const {number}
 */
ApplicationCache.prototype.CHECKING;

/**
 * The update has started but the resources are not downloaded yet - for
 * example, this can happen when the manifest file is fetched.
 * @const {number}
 */
ApplicationCache.CHECKING;

/**
 * The resources are being downloaded into the cache.
 * @const {number}
 */
ApplicationCache.prototype.DOWNLOADING;

/**
 * The resources are being downloaded into the cache.
 * @const {number}
 */
ApplicationCache.DOWNLOADING;

/**
 * Resources have finished downloading and the new cache is ready to be used.
 * @const {number}
 */
ApplicationCache.prototype.UPDATEREADY;

/**
 * Resources have finished downloading and the new cache is ready to be used.
 * @const {number}
 */
ApplicationCache.UPDATEREADY;

/**
 * The cache is obsolete.
 * @const {number}
 */
ApplicationCache.prototype.OBSOLETE;

/**
 * The cache is obsolete.
 * @const {number}
 */
ApplicationCache.OBSOLETE;

/**
 * The current status of the application cache.
 * @type {number}
 */
ApplicationCache.prototype.status;

/**
 * Sent when the update process finishes for the first time; that is, the first
 * time an application cache is saved.
 * @type {?function(!Event): void}
 */
ApplicationCache.prototype.oncached;

/**
 * Sent when the cache update process begins.
 * @type {?function(!Event): void}
 */
ApplicationCache.prototype.onchecking;

/**
 * Sent when the update process begins downloading resources in the manifest
 * file.
 * @type {?function(!Event): void}
 */
ApplicationCache.prototype.ondownloading;

/**
 * Sent when an error occurs.
 * @type {?function(!Event): void}
 */
ApplicationCache.prototype.onerror;

/**
 * Sent when the update process finishes but the manifest file does not change.
 * @type {?function(!Event): void}
 */
ApplicationCache.prototype.onnoupdate;

/**
 * Sent when each resource in the manifest file begins to download.
 * @type {?function(!Event): void}
 */
ApplicationCache.prototype.onprogress;

/**
 * Sent when there is an existing application cache, the update process
 * finishes, and there is a new application cache ready for use.
 * @type {?function(!Event): void}
 */
ApplicationCache.prototype.onupdateready;

/**
 * Replaces the active cache with the latest version.
 * @throws {DOMException}
 * @return {undefined}
 */
ApplicationCache.prototype.swapCache = function() {};

/**
 * Manually triggers the update process.
 * @throws {DOMException}
 * @return {undefined}
 */
ApplicationCache.prototype.update = function() {};

/** @type {?ApplicationCache} */
var applicationCache;

/** @type {ApplicationCache} */
Window.prototype.applicationCache;

/**
 * @see https://developer.mozilla.org/En/DOM/Worker/Functions_available_to_workers
 * @param {...!TrustedScriptURL|!URL|string} urls
 * @return {undefined}
 */
Window.prototype.importScripts = function(urls) {};

/**
 * Decodes a string of data which has been encoded using base-64 encoding.
 *
 * @param {string} encodedData
 * @return {string}
 * @nosideeffects
 * @see https://html.spec.whatwg.org/multipage/webappapis.html#dom-atob
 */
function atob(encodedData) {}

/**
 * @param {string} stringToEncode
 * @return {string}
 * @nosideeffects
 * @see https://html.spec.whatwg.org/multipage/webappapis.html#dom-btoa
 */
function btoa(stringToEncode) {}

/**
 * @see https://developer.mozilla.org/En/DOM/Worker/Functions_available_to_workers
 * @param {...!TrustedScriptURL|!URL|string} urls
 * @return {undefined}
 */
function importScripts(urls) {}

/**
 * @see http://dev.w3.org/html5/workers/
 * @param {!TrustedScriptURL|!URL|string} scriptURL
 * @param {!WorkerOptions=} opt_options
 * @constructor
 * @implements {EventTarget}
 */
function Worker(scriptURL, opt_options) {}

/** @override */
Worker.prototype.addEventListener = function(type, listener, opt_options) {};

/** @override */
Worker.prototype.removeEventListener = function(type, listener, opt_options) {};

/** @override */
Worker.prototype.dispatchEvent = function(evt) {};

/**
 * Stops the worker process
 * @return {undefined}
 */
Worker.prototype.terminate = function() {};

/**
 * Posts a message to the worker thread.
 * @see https://html.spec.whatwg.org/multipage/workers.html#dom-worker-postmessage
 * @param {*} message
 * @param {(Array<!Transferable>|!StructuredSerializeOptions)=}
 *     transferOrOptions
 * @return {undefined}
 */
Worker.prototype.postMessage = function(message, transferOrOptions) {};

/**
 * Posts a message to the worker thread.
 * @param {*} message
 * @param {(Array<!Transferable>|!StructuredSerializeOptions)=}
 *     transferOrOptions
 * @return {undefined}
 */
Worker.prototype.webkitPostMessage = function(message, transferOrOptions) {};

/**
 * Sent when the worker thread posts a message to its creator.
 * @type {?function(!MessageEvent<*>): void}
 */
Worker.prototype.onmessage;

/**
 * @type {?function(!MessageEvent)}
 * @see https://developer.mozilla.org/docs/Web/API/Worker/messageerror_event
 */
Worker.prototype.onmessageerror;

/**
 * Sent when the worker thread encounters an error.
 * @type {?function(!ErrorEvent): void}
 */
Worker.prototype.onerror;

/**
 * @see http://dev.w3.org/html5/workers/
 * @record
 */
function WorkerOptions() {}

/**
 * Defines a name for the new global environment of the worker, primarily for
 * debugging purposes.
 * @type {string|undefined}
 */
WorkerOptions.prototype.name;

/**
 * 'classic' or 'module'. Default: 'classic'.
 * Specifying 'module' ensures the worker environment supports JavaScript
 * modules.
 * @type {string|undefined}
 */
WorkerOptions.prototype.type;

// WorkerOptions.prototype.credentials is defined in fetchapi.js.
// if type = 'module', it specifies how scriptURL is fetched.

/**
 * @see http://dev.w3.org/html5/workers/
 * @param {!TrustedScriptURL|!URL|string} scriptURL The URL of the script to run
 *     in the SharedWorker.
 * @param {(string|!WorkerOptions)=} options A name that can
 *     later be used to obtain a reference to the same SharedWorker or a
 *     WorkerOptions object which can be be used to specify how scriptURL is
 *     fetched through the credentials option.
 * @constructor
 * @implements {EventTarget}
 */
function SharedWorker(scriptURL, options) {}

/** @override */
SharedWorker.prototype.addEventListener = function(
    type, listener, opt_options) {};

/** @override */
SharedWorker.prototype.removeEventListener = function(
    type, listener, opt_options) {};

/** @override */
SharedWorker.prototype.dispatchEvent = function(evt) {};

/**
 * @type {!MessagePort}
 */
SharedWorker.prototype.port;

/**
 * Called on network errors for loading the initial script.
 * @type {?function(!ErrorEvent): void}
 */
SharedWorker.prototype.onerror;

/**
 * @see http://dev.w3.org/html5/workers/
 * @see http://www.w3.org/TR/url-1/#dom-urlutilsreadonly
 * @interface
 */
function WorkerLocation() {}

/** @type {string} */
WorkerLocation.prototype.href;

/** @type {string} */
WorkerLocation.prototype.origin;

/** @type {string} */
WorkerLocation.prototype.protocol;

/** @type {string} */
WorkerLocation.prototype.host;

/** @type {string} */
WorkerLocation.prototype.hostname;

/** @type {string} */
WorkerLocation.prototype.port;

/** @type {string} */
WorkerLocation.prototype.pathname;

/** @type {string} */
WorkerLocation.prototype.search;

/** @type {string} */
WorkerLocation.prototype.hash;

/**
 * @see http://dev.w3.org/html5/workers/
 * @interface
 * @extends {EventTarget}
 */
function WorkerGlobalScope() {}

/** @type {!WorkerGlobalScope} */
WorkerGlobalScope.prototype.self;

/** @type {!WorkerLocation} */
WorkerGlobalScope.prototype.location;

/**
 * @const {string}
 * @see https://html.spec.whatwg.org/#windoworworkerglobalscope-mixin
 */
WorkerGlobalScope.prototype.origin;

/**
 * @const {string}
 * Duplicate definition, since we don't model WindowOrWorkerGlobalScope.
 * @see https://html.spec.whatwg.org/#windoworworkerglobalscope-mixin
 */
Window.prototype.origin;

/**
 * Closes the worker represented by this WorkerGlobalScope.
 * @return {undefined}
 */
WorkerGlobalScope.prototype.close = function() {};

/**
 * Sent when the worker encounters an error.
 * @type {?function(string, string, number, number, !Error): void}
 */
WorkerGlobalScope.prototype.onerror;

/**
 * Sent when the worker goes offline.
 * @type {?function(!Event): void}
 */
WorkerGlobalScope.prototype.onoffline;

/**
 * Sent when the worker goes online.
 * @type {?function(!Event): void}
 */
WorkerGlobalScope.prototype.ononline;

/** @type {!WorkerPerformance} */
WorkerGlobalScope.prototype.performance;

/** @type {!WorkerNavigator} */
WorkerGlobalScope.prototype.navigator;

/**
 * Worker postMessage method.
 * @see https://html.spec.whatwg.org/multipage/workers.html#dom-dedicatedworkerglobalscope-postmessage
 * @param {*} message
 * @param {(!Array<!Transferable>|!StructuredSerializeOptions)=}
 *     transferOrOptions
 * @return {void}
 */
WorkerGlobalScope.prototype.postMessage = function(
    message, transferOrOptions) {};

/**
 * @see http://dev.w3.org/html5/workers/
 * @interface
 * @extends {WorkerGlobalScope}
 */
function DedicatedWorkerGlobalScope() {}

/**
 * Posts a message to creator of this worker.
 * @see https://html.spec.whatwg.org/multipage/workers.html#dom-dedicatedworkerglobalscope-postmessage
 * @param {*} message
 * @param {(Array<!Transferable>|!StructuredSerializeOptions)=}
 *     transferOrOptions
 * @return {undefined}
 */
DedicatedWorkerGlobalScope.prototype.postMessage = function(
    message, transferOrOptions) {};

/**
 * Posts a message to creator of this worker.
 * @param {*} message
 * @param {(Array<!Transferable>|!StructuredSerializeOptions)=}
 *     transferOrOptions
 * @return {undefined}
 */
DedicatedWorkerGlobalScope.prototype.webkitPostMessage = function(
    message, transferOrOptions) {};

/**
 * Sent when the creator posts a message to this worker.
 * @type {?function(!MessageEvent<*>): void}
 */
DedicatedWorkerGlobalScope.prototype.onmessage;

/**
 * @see http://dev.w3.org/html5/workers/
 * @interface
 * @extends {WorkerGlobalScope}
 */
function SharedWorkerGlobalScope() {}

/** @type {string} */
SharedWorkerGlobalScope.prototype.name;

/**
 * Sent when a connection to this worker is opened.
 * @type {?function(!MessageEvent)}
 */
SharedWorkerGlobalScope.prototype.onconnect;

/** @type {!Array<string>|undefined} */
HTMLElement.observedAttributes;

/**
 * @see https://html.spec.whatwg.org/multipage/custom-elements.html#custom-elements-face-example
 * @type {boolean|undefined}
 */
HTMLElement.formAssociated;

/**
 * @param {!Document} oldDocument
 * @param {!Document} newDocument
 */
HTMLElement.prototype.adoptedCallback = function(oldDocument, newDocument) {};

/**
 * @param {!ShadowRootInit} options
 * @return {!ShadowRoot}
 */
HTMLElement.prototype.attachShadow = function(options) {};

/**
 * @return {!ElementInternals}
 */
HTMLElement.prototype.attachInternals = function() {};

/**
 * @param {string} attributeName
 * @param {?string} oldValue
 * @param {?string} newValue
 * @param {?string} namespace
 */
HTMLElement.prototype.attributeChangedCallback = function(
    attributeName, oldValue, newValue, namespace) {};

/** @type {function()|undefined} */
HTMLElement.prototype.connectedCallback;

/** @type {Element} */
HTMLElement.prototype.contextMenu;

/** @type {function()|undefined} */
HTMLElement.prototype.disconnectedCallback;

/** @type {boolean} */
HTMLElement.prototype.draggable;

/**
 * This is actually a DOMSettableTokenList property. However since that
 * interface isn't currently defined and no known browsers implement this
 * feature, just define the property for now.
 *
 * @const {Object}
 */
HTMLElement.prototype.dropzone;

/** @type {boolean} */
HTMLElement.prototype.hidden;

/** @type {boolean} */
HTMLElement.prototype.inert;

/** @type {boolean} */
HTMLElement.prototype.spellcheck;

/**
 * @see https://html.spec.whatwg.org/multipage/custom-elements.html#custom-elements-face-example
 * @param {HTMLFormElement} form
 */
HTMLElement.prototype.formAssociatedCallback = function(form) {};

/**
 * @see https://html.spec.whatwg.org/multipage/custom-elements.html#custom-elements-face-example
 * @param {boolean} disabled
 */
HTMLElement.prototype.formDisabledCallback = function(disabled) {};

/**
 * @see https://html.spec.whatwg.org/multipage/custom-elements.html#custom-elements-face-example
 * @type {function()|undefined}
 */
HTMLElement.prototype.formResetCallback;

/**
 * @param {null|string|!File|!Array<!Array<string|!File>>} state
 * @param {string} reason
 */
HTMLElement.prototype.formStateRestoreCallback = function(state, reason) {};

/**
 * @see https://dom.spec.whatwg.org/#dictdef-getrootnodeoptions
 * @typedef {{
 *   composed: boolean
 * }}
 */
var GetRootNodeOptions;

/**
 * @see https://dom.spec.whatwg.org/#dom-node-getrootnode
 * @param {GetRootNodeOptions=} opt_options
 * @return {!Node}
 */
Node.prototype.getRootNode = function(opt_options) {};

/**
 * @see http://www.w3.org/TR/components-intro/
 * @return {!ShadowRoot}
 */
HTMLElement.prototype.createShadowRoot;

/**
 * @see http://www.w3.org/TR/components-intro/
 * @return {!ShadowRoot}
 */
HTMLElement.prototype.webkitCreateShadowRoot;

/**
 * @see http://www.w3.org/TR/shadow-dom/
 * @type {ShadowRoot}
 */
HTMLElement.prototype.shadowRoot;

/**
 * @see http://www.w3.org/TR/shadow-dom/
 * @return {!NodeList<!Node>}
 */
HTMLElement.prototype.getDestinationInsertionPoints = function() {};

/**
 * @see http://www.w3.org/TR/components-intro/
 * @type {function()}
 */
HTMLElement.prototype.createdCallback;

/**
 * @see http://w3c.github.io/webcomponents/explainer/#lifecycle-callbacks
 * @type {function()}
 */
HTMLElement.prototype.attachedCallback;

/**
 * @see http://w3c.github.io/webcomponents/explainer/#lifecycle-callbacks
 * @type {function()}
 */
HTMLElement.prototype.detachedCallback;

/**
 * Cryptographic nonce used by Content Security Policy.
 * @see https://html.spec.whatwg.org/multipage/dom.html#elements-in-the-dom:noncedelement
 * @type {?string}
 */
HTMLElement.prototype.nonce;

/** @type {string} */
HTMLAnchorElement.prototype.download;

/** @type {string} */
HTMLAnchorElement.prototype.hash;

/** @type {string} */
HTMLAnchorElement.prototype.host;

/** @type {string} */
HTMLAnchorElement.prototype.hostname;

/** @type {string} */
HTMLAnchorElement.prototype.pathname;

/**
 * The 'ping' attribute is known to be supported in recent versions (as of
 * mid-2014) of Chrome, Safari, and Firefox, and is not supported in any
 * current version of Internet Explorer.
 *
 * @type {string}
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/semantics.html#hyperlink-auditing
 */
HTMLAnchorElement.prototype.ping;

/** @type {string} */
HTMLAnchorElement.prototype.port;

/** @type {string} */
HTMLAnchorElement.prototype.protocol;

/** @type {!DOMTokenList} */
HTMLAnchorElement.prototype.relList;

/** @type {string} */
HTMLAnchorElement.prototype.search;

/** @type {string} */
HTMLAreaElement.prototype.download;

/**
 * @type {string}
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/semantics.html#hyperlink-auditing
 */
HTMLAreaElement.prototype.ping;

/**
 * @type {string}
 * @implicitCast
 * @see http://www.w3.org/TR/html-markup/iframe.html#iframe.attrs.srcdoc
 */
HTMLIFrameElement.prototype.srcdoc;

/**
 * @type {?DOMTokenList}
 * @see http://www.w3.org/TR/2012/WD-html5-20121025/the-iframe-element.html#attr-iframe-sandbox
 */
HTMLIFrameElement.prototype.sandbox;

/**
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/iframe-embed-object.html#attr-iframe-allow
 */
HTMLIFrameElement.prototype.allow;

/**
 * @type {boolean}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/HTMLIFrameElement/allowFullscreen
 */
HTMLIFrameElement.prototype.allowFullscreen;

/**
 * @type {Window}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/HTMLIFrameElement/contentWindow
 */
HTMLIFrameElement.prototype.contentWindow;

/**
 * @return {?Document}
 * @see https://developer.mozilla.org/docs/Web/API/HTMLIframeElement/getSVGDocument
 */
HTMLIFrameElement.prototype.getSVGDocument= function() {};

/** @type {string} */
HTMLInputElement.prototype.autocomplete;

/** @type {string} */
HTMLInputElement.prototype.dirname;

/** @type {FileList} */
HTMLInputElement.prototype.files;

/**
 * @type {boolean}
 * @see https://www.w3.org/TR/html5/forms.html#dom-input-indeterminate
 */
HTMLInputElement.prototype.indeterminate;

/** @type {string} */
HTMLInputElement.prototype.list;

/** @implicitCast @type {string} */
HTMLInputElement.prototype.max;

/** @implicitCast @type {string} */
HTMLInputElement.prototype.min;

/** @type {string} */
HTMLInputElement.prototype.pattern;

/** @type {boolean} */
HTMLInputElement.prototype.multiple;

/** @type {string} */
HTMLInputElement.prototype.placeholder;

/** @type {boolean} */
HTMLInputElement.prototype.required;

/** @implicitCast @type {string} */
HTMLInputElement.prototype.step;

/** @type {Date} */
HTMLInputElement.prototype.valueAsDate;

/** @type {number} */
HTMLInputElement.prototype.valueAsNumber;

/** @type {!Array<!FileSystemEntry>} */
HTMLInputElement.prototype.webkitEntries;

/** @type {boolean} */
HTMLInputElement.prototype.webkitdirectory;

/**
 * Changes the form control's value by the value given in the step attribute
 * multiplied by opt_n.
 * @param {number=} opt_n step multiplier.  Defaults to 1.
 * @return {undefined}
 */
HTMLInputElement.prototype.stepDown = function(opt_n) {};

/**
 * Changes the form control's value by the value given in the step attribute
 * multiplied by opt_n.
 * @param {number=} opt_n step multiplier.  Defaults to 1.
 * @return {undefined}
 */
HTMLInputElement.prototype.stepUp = function(opt_n) {};

/**
 * Displays the browser picker for an input element.
 * @return {undefined}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/HTMLInputElement/showPicker
 */
HTMLInputElement.prototype.showPicker = function() {};


/**
 * @constructor
 * @implements {EventTarget}
 */
function RemotePlayback() {}

/** @type {?function(!Event)} */
RemotePlayback.prototype.onconnect;

/** @type {?function(!Event)} */
RemotePlayback.prototype.onconnecting;

/** @type {?function(!Event)} */
RemotePlayback.prototype.ondisconnect;

/** @type {string} */
RemotePlayback.prototype.state;

/**
 * @param {function(boolean):void} callback
 * @return {!Promise<number>}
 */
RemotePlayback.prototype.watchAvailability = function(callback) {};

/**
 * @param {number} id
 * @return {!Promise<void>}
 */
RemotePlayback.prototype.cancelWatchAvailability = function(id) {};

/**
 * @return {!Promise<void>}
 */
RemotePlayback.prototype.prompt = function() {};

/** @override */
RemotePlayback.prototype.addEventListener = function(
  type, listener, opt_useCapture) {};

/** @override */
RemotePlayback.prototype.dispatchEvent = function(evt) {};

/** @override */
RemotePlayback.prototype.removeEventListener = function(
  type, listener, opt_options) {};

/**
 * @constructor
 * @extends {HTMLElement}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement
 */
function HTMLMediaElement() {}

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-network_empty
 * @const {number}
 */
HTMLMediaElement.NETWORK_EMPTY;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-network_empty
 * @const {number}
 */
HTMLMediaElement.prototype.NETWORK_EMPTY;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-network_idle
 * @const {number}
 */
HTMLMediaElement.NETWORK_IDLE;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-network_idle
 * @const {number}
 */
HTMLMediaElement.prototype.NETWORK_IDLE;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-network_loading
 * @const {number}
 */
HTMLMediaElement.NETWORK_LOADING;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-network_loading
 * @const {number}
 */
HTMLMediaElement.prototype.NETWORK_LOADING;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-network_no_source
 * @const {number}
 */
HTMLMediaElement.NETWORK_NO_SOURCE;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-network_no_source
 * @const {number}
 */
HTMLMediaElement.prototype.NETWORK_NO_SOURCE;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-have_nothing
 * @const {number}
 */
HTMLMediaElement.HAVE_NOTHING;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-have_nothing
 * @const {number}
 */
HTMLMediaElement.prototype.HAVE_NOTHING;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-have_metadata
 * @const {number}
 */
HTMLMediaElement.HAVE_METADATA;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-have_metadata
 * @const {number}
 */
HTMLMediaElement.prototype.HAVE_METADATA;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-have_current_data
 * @const {number}
 */
HTMLMediaElement.HAVE_CURRENT_DATA;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-have_current_data
 * @const {number}
 */
HTMLMediaElement.prototype.HAVE_CURRENT_DATA;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-have_future_data
 * @const {number}
 */
HTMLMediaElement.HAVE_FUTURE_DATA;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-have_future_data
 * @const {number}
 */
HTMLMediaElement.prototype.HAVE_FUTURE_DATA;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-have_enough_data
 * @const {number}
 */
HTMLMediaElement.HAVE_ENOUGH_DATA;

/**
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-have_enough_data
 * @const {number}
 */
HTMLMediaElement.prototype.HAVE_ENOUGH_DATA;

/** @type {MediaError} */
HTMLMediaElement.prototype.error;

/** @type {string} @implicitCast */
HTMLMediaElement.prototype.src;

/** @type {string} */
HTMLMediaElement.prototype.currentSrc;

/** @type {number} */
HTMLMediaElement.prototype.networkState;

/** @type {boolean} */
HTMLMediaElement.prototype.autobuffer;

/** @type {!TimeRanges} */
HTMLMediaElement.prototype.buffered;

/** @type {?MediaStream} */
HTMLMediaElement.prototype.srcObject;

/** @type {boolean} */
HTMLMediaElement.prototype.defaultMuted;

/** @type {boolean} */
HTMLMediaElement.prototype.disableRemotePlayback;

/** @type {!RemotePlayback} */
HTMLMediaElement.prototype.remote;

/** @type {boolean} */
HTMLMediaElement.prototype.preservesPitch;

/**
 * Loads the media element.
 * @return {undefined}
 */
HTMLMediaElement.prototype.load = function() {};

/**
 * @param {string} type Type of the element in question in question.
 * @return {string} Whether it can play the type.
 * @nosideeffects
 */
HTMLMediaElement.prototype.canPlayType = function(type) {};

/** Event handlers */

/** @type {?function(Event)} */
HTMLMediaElement.prototype.onabort;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.oncanplay;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.oncanplaythrough;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.ondurationchange;

/** @type {?function(!MediaEncryptedEvent)} */
HTMLMediaElement.prototype.onencrypted;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onwaitingforkey;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onemptied;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onended;

/** @type {?function(Event)} */
HTMLMediaElement.prototype.onerror;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onloadeddata;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onloadedmetadata;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onloadstart;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onpause;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onplay;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onplaying;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onprogress;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onratechange;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onseeked;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onseeking;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onstalled;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onsuspend;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.ontimeupdate;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onvolumechange;

/** @type {?function(!Event)} */
HTMLMediaElement.prototype.onwaiting;

/** @type {?function(Event)} */
HTMLImageElement.prototype.onload;

/** @type {?function(Event)} */
HTMLImageElement.prototype.onerror;

/**
 * @type {string}
 * @deprecated
*/
HTMLImageElement.prototype.lowsrc;

/** @type {string} */
HTMLMediaElement.prototype.preload;

/** @type {number} */
HTMLMediaElement.prototype.readyState;

/** @type {boolean} */
HTMLMediaElement.prototype.seeking;

/**
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-media-crossorigin
 */
HTMLMediaElement.prototype.crossOrigin;

/**
 * The current time, in seconds.
 * @type {number}
 */
HTMLMediaElement.prototype.currentTime;

/**
 * The absolute timeline offset.
 * @return {!Date}
 */
HTMLMediaElement.prototype.getStartDate = function() {};

/**
 * The length of the media in seconds.
 * @type {number}
 */
HTMLMediaElement.prototype.duration;

/** @type {boolean} */
HTMLMediaElement.prototype.paused;

/** @type {number} */
HTMLMediaElement.prototype.defaultPlaybackRate;

/** @type {number} */
HTMLMediaElement.prototype.playbackRate;

/** @type {TimeRanges} */
HTMLMediaElement.prototype.played;

/** @type {TimeRanges} */
HTMLMediaElement.prototype.seekable;

/** @type {boolean} */
HTMLMediaElement.prototype.ended;

/** @type {boolean} */
HTMLMediaElement.prototype.autoplay;

/** @type {boolean} */
HTMLMediaElement.prototype.loop;

/**
 * Starts playing the media.
 * @return {?Promise<undefined>} This is a *nullable* Promise on purpose unlike
 *     the HTML5 spec because supported older browsers (incl. Smart TVs) don't
 *     return a Promise.
 */
HTMLMediaElement.prototype.play = function() {};

/**
 * Pauses the media.
 * @return {undefined}
 */
HTMLMediaElement.prototype.pause = function() {};

/** @type {boolean} */
HTMLMediaElement.prototype.controls;

/**
 * The audio volume, from 0.0 (silent) to 1.0 (loudest).
 * @type {number}
 */
HTMLMediaElement.prototype.volume;

/** @type {boolean} */
HTMLMediaElement.prototype.muted;

/**
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/the-video-element.html#dom-media-addtexttrack
 * @param {string} kind Kind of the text track.
 * @param {string=} opt_label Label of the text track.
 * @param {string=} opt_language Language of the text track.
 * @return {!TextTrack} TextTrack object added to the media element.
 */
HTMLMediaElement.prototype.addTextTrack = function(
    kind, opt_label, opt_language) {};

/** @type {!TextTrackList} */
HTMLMediaElement.prototype.textTracks;

/**
 * The ID of the audio device through which output is being delivered, or an
 * empty string if using the default device.
 *
 * Implemented as a draft spec in Chrome 49+.
 *
 * @see https://w3c.github.io/mediacapture-output/#htmlmediaelement-extensions
 * @type {string}
 */
HTMLMediaElement.prototype.sinkId;

/**
 * Sets the audio device through which output should be delivered.
 *
 * Implemented as a draft spec in Chrome 49+.
 *
 * @param {string} sinkId The ID of the audio output device, or empty string
 * for default device.
 *
 * @see https://w3c.github.io/mediacapture-output/#htmlmediaelement-extensions
 * @return {!Promise<void>}
 */
HTMLMediaElement.prototype.setSinkId = function(sinkId) {};


/**
 * Produces a real-time capture of the media that is rendered to the media
 * element.
 * @return {!MediaStream}
 * @see https://w3c.github.io/mediacapture-fromelement/#html-media-element-media-capture-extensions
 */
HTMLMediaElement.prototype.captureStream = function() {};

/**
 * The Firefox flavor of captureStream.
 * @return {!MediaStream}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement/captureStream#Browser_compatibility
 */
HTMLMediaElement.prototype.mozCaptureStream = function() {};

/**
 * @constructor
 * @extends {HTMLElement}
 * @see https://html.spec.whatwg.org/multipage/dom.html#htmlunknownelement
 * @see https://html.spec.whatwg.org/multipage/scripting.html#customized-built-in-element-restrictions
 * @see https://w3c.github.io/webcomponents/spec/custom/#custom-elements-api
 */
function HTMLUnknownElement() {}



/**
 * @see http://www.w3.org/TR/shadow-dom/
 * @return {!NodeList<!Node>}
 */
Text.prototype.getDestinationInsertionPoints = function() {};


/**
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/the-video-element.html#texttracklist
 * @constructor
 * @implements {EventTarget}
 * @implements {IArrayLike<!TextTrack>}
 */
function TextTrackList() {}

/** @type {number} */
TextTrackList.prototype.length;

/**
 * @param {string} id
 * @return {TextTrack}
 */
TextTrackList.prototype.getTrackById = function(id) {};

/** @override */
TextTrackList.prototype.addEventListener = function(
    type, listener, opt_useCapture) {};

/** @override */
TextTrackList.prototype.dispatchEvent = function(evt) {};

/** @override */
TextTrackList.prototype.removeEventListener = function(
    type, listener, opt_options) {};

/**
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/the-video-element.html#texttrack
 * @constructor
 * @implements {EventTarget}
 */
function TextTrack() {}

/**
 * @param {TextTrackCue} cue
 * @return {undefined}
 */
TextTrack.prototype.addCue = function(cue) {};

/**
 * @param {TextTrackCue} cue
 * @return {undefined}
 */
TextTrack.prototype.removeCue = function(cue) {};

/**
 * @type {?function(!Event)}
 * @see https://developer.mozilla.org/docs/Web/API/TextTrack/cuechange_event
 */
TextTrack.prototype.oncuechange;

/**
 * @const {TextTrackCueList}
 */
TextTrack.prototype.activeCues;

/**
 * @const {TextTrackCueList}
 */
TextTrack.prototype.cues;

/**
 * @const {string}
 * @see https://html.spec.whatwg.org/multipage/media.html#dom-texttrack-id-dev
 */
TextTrack.prototype.id;

/**
 * @type {string}
 * @see https://developer.mozilla.org/docs/Web/API/TextTrack/inBandMetadataTrackDispatchType
 */
TextTrack.prototype.inBandMetadataTrackDispatchType;

/**
 * @type {string}
 * @see https://developer.mozilla.org/docs/Web/API/TextTrack/kind
 */
TextTrack.prototype.kind;

/**
 * @type {string}
 * @see https://developer.mozilla.org/docs/Web/API/TextTrack/label
 */
TextTrack.prototype.label;

/**
 * @type {string}
 * @see https://developer.mozilla.org/docs/Web/API/TextTrack/language
 */
TextTrack.prototype.language;

/**
 * @type {string}
 */
TextTrack.prototype.mode;

/** @override */
TextTrack.prototype.addEventListener = function(
    type, listener, opt_useCapture) {};

/** @override */
TextTrack.prototype.dispatchEvent = function(evt) {};

/** @override */
TextTrack.prototype.removeEventListener = function(
    type, listener, opt_options) {};



/**
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/the-video-element.html#texttrackcuelist
 * @constructor
 * @implements {IArrayLike<!TextTrackCue>}
 */
function TextTrackCueList() {}

/** @const {number} */
TextTrackCueList.prototype.length;

/**
 * @param {string} id
 * @return {TextTrackCue}
 */
TextTrackCueList.prototype.getCueById = function(id) {};



/**
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/the-video-element.html#texttrackcue
 * @constructor
 * @param {number} startTime
 * @param {number} endTime
 * @param {string} text
 */
function TextTrackCue(startTime, endTime, text) {}

/** @type {string} */
TextTrackCue.prototype.id;

/** @type {number} */
TextTrackCue.prototype.startTime;

/** @type {number} */
TextTrackCue.prototype.endTime;

/** @type {string} */
TextTrackCue.prototype.text;

/** @type {?function(!Event)} */
TextTrackCue.prototype.onenter;

/** @type {?function(!Event)} */
TextTrackCue.prototype.onexit;

/** @type {boolean} */
TextTrackCue.prototype.pauseOnExit;


/**
 * @see https://w3c.github.io/webvtt/#vttregion
 * @constructor
 */
function VTTRegion() {}

/** @type {string} */
VTTRegion.prototype.id;

/** @type {number} */
VTTRegion.prototype.width;

/** @type {number} */
VTTRegion.prototype.lines;

/** @type {number} */
VTTRegion.prototype.regionAnchorX;

/** @type {number} */
VTTRegion.prototype.regionAnchorY;

/** @type {number} */
VTTRegion.prototype.viewportAnchorX;

/** @type {number} */
VTTRegion.prototype.viewportAnchorY;

/**
 * @see https://w3c.github.io/webvtt/#enumdef-scrollsetting
 * @type {string}
 */
VTTRegion.prototype.scroll;



/**
 * @see http://dev.w3.org/html5/webvtt/#the-vttcue-interface
 * @constructor
 * @extends {TextTrackCue}
 * @param {number} startTime
 * @param {number} endTime
 * @param {string} text
 */
function VTTCue(startTime, endTime, text) {}

/** @type {?VTTRegion} */
VTTCue.prototype.region;

/**
 * @see https://w3c.github.io/webvtt/#enumdef-directionsetting
 * @type {string}
 */
VTTCue.prototype.vertical;

/** @type {boolean} */
VTTCue.prototype.snapToLines;

/** @type {(number|string)} */
VTTCue.prototype.line;

/**
 * @see https://w3c.github.io/webvtt/#enumdef-linealignsetting
 * @type {string}
 */
VTTCue.prototype.lineAlign;

/** @type {(number|string)} */
VTTCue.prototype.position;

/**
 * @see https://w3c.github.io/webvtt/#enumdef-positionalignsetting
 * @type {string}
 */
VTTCue.prototype.positionAlign;

/** @type {number} */
VTTCue.prototype.size;

/**
 * @see https://w3c.github.io/webvtt/#enumdef-alignsetting
 * @type {string}
 */
VTTCue.prototype.align;

/** @type {string} */
VTTCue.prototype.text;

/** @return {!DocumentFragment} */
VTTCue.prototype.getCueAsHTML = function() {};


/**
 * @constructor
 * @extends {HTMLMediaElement}
 */
function HTMLAudioElement() {}

/**
 * @constructor
 * @extends {HTMLMediaElement}
 * The webkit-prefixed attributes are defined in
 * https://cs.chromium.org/chromium/src/third_party/WebKit/Source/core/html/media/HTMLMediaElement.idl
 */
function HTMLVideoElement() {}

/**
 * Starts displaying the video in full screen mode.
 * @return {undefined}
 */
HTMLVideoElement.prototype.webkitEnterFullscreen = function() {};

/**
 * Starts displaying the video in full screen mode.
 * @return {undefined}
 */
HTMLVideoElement.prototype.webkitEnterFullScreen = function() {};

/**
 * Stops displaying the video in full screen mode.
 * @return {undefined}
 */
HTMLVideoElement.prototype.webkitExitFullscreen = function() {};

/**
 * Stops displaying the video in full screen mode.
 * @return {undefined}
 */
HTMLVideoElement.prototype.webkitExitFullScreen = function() {};

/** @type {number} */
HTMLVideoElement.prototype.width;

/** @type {number} */
HTMLVideoElement.prototype.height;

/** @type {number} */
HTMLVideoElement.prototype.videoWidth;

/** @type {number} */
HTMLVideoElement.prototype.videoHeight;

/** @type {boolean} */
HTMLVideoElement.prototype.playsInline;

/** @type {string} */
HTMLVideoElement.prototype.poster;

/** @type {boolean} */
HTMLVideoElement.prototype.webkitSupportsFullscreen;

/** @type {boolean} */
HTMLVideoElement.prototype.webkitDisplayingFullscreen;

/** @type {number} */
HTMLVideoElement.prototype.webkitDecodedFrameCount;

/** @type {number} */
HTMLVideoElement.prototype.webkitDroppedFrameCount;

/**
 * @typedef {{
 *    creationTime: number,
 *    totalVideoFrames: number,
 *    droppedVideoFrames: number,
 *    corruptedVideoFrames: number,
 *    totalFrameDelay: number,
 *    displayCompositedVideoFrames: (number|undefined)
 * }}
 */
var VideoPlaybackQuality;

/**
 * @see https://w3c.github.io/media-source/#htmlvideoelement-extensions
 * @return {!VideoPlaybackQuality} Stats about the current playback.
 */
HTMLVideoElement.prototype.getVideoPlaybackQuality = function() {};


/**
 * The metadata provided by the callback given to
 * HTMLVideoElement.requestVideoFrameCallback().
 *
 * See https://wicg.github.io/video-rvfc/#video-frame-metadata
 *
 * @record
 */
function VideoFrameMetadata() {};

/**
 * The time at which the user agent submitted the frame for composition.
 * @const {number}
 */
VideoFrameMetadata.prototype.presentationTime;

/**
 * The time at which the user agent expects the frame to be visible.
 * @const {number}
 */
VideoFrameMetadata.prototype.expectedDisplayTime;

/**
 * The width of the video frame, in media pixels.
 * @const {number}
 */
VideoFrameMetadata.prototype.width;

/**
 * The height of the video frame, in media pixels.
 * @const {number}
 */
VideoFrameMetadata.prototype.height;

/**
 * The media presentation timestamp (PTS) in seconds of the frame presented
 * (e.g. its timestamp on the video.currentTime timeline).
 * @const {number}
 */
VideoFrameMetadata.prototype.mediaTime;

/**
 * A count of the number of frames submitted for composition.
 * @const {number}
 */
VideoFrameMetadata.prototype.presentedFrames;

/**
 * The elapsed duration in seconds from submission of the encoded packet with
 * the same presentation timestamp (PTS) as this frame (e.g. same as the
 * mediaTime) to the decoder until the decoded frame was ready for presentation.
 * @const {number|undefined}
 */
VideoFrameMetadata.prototype.processingDuration;

/**
 * For video frames coming from either a local or remote source, this is the
 * time at which the frame was captured by the camera.
 * @const {number|undefined}
 */
VideoFrameMetadata.prototype.captureTime;

/**
 * For video frames coming from a remote source, this is the time the encoded
 * frame was received by the platform, i.e., the time at which the last packet
 * belonging to this frame was received over the network.
 * @const {number|undefined}
 */
VideoFrameMetadata.prototype.receiveTime;

/**
 * The RTP timestamp associated with this video frame.
 * @const {number|undefined}
 */
VideoFrameMetadata.prototype.rtpTimestamp;

/**
 * @typedef {function(number,  ?VideoFrameMetadata): undefined}
 * @see https://wicg.github.io/video-rvfc/#dom-htmlvideoelement-requestvideoframecallback
 */
var VideoFrameRequestCallback;

/**
 * Registers a callback to be fired the next time a frame is presented to the
 * compositor.
 * @param {!VideoFrameRequestCallback} callback
 * @return {number}
 */
HTMLVideoElement.prototype.requestVideoFrameCallback = function(callback) {};

/**
 * Cancels an existing video frame request callback given its handle.
 * @param {number} handle
 * @return {undefined}
 */
HTMLVideoElement.prototype.cancelVideoFrameCallback = function(handle) {};


/**
 * @constructor
 * @see https://html.spec.whatwg.org/multipage/media.html#error-codes
 */
function MediaError() {}

/** @type {number} */
MediaError.prototype.code;

/** @type {string} */
MediaError.prototype.message;

/**
 * The fetching process for the media resource was aborted by the user agent at
 * the user's request.
 * @const {number}
 */
MediaError.MEDIA_ERR_ABORTED;

/**
 * A network error of some description caused the user agent to stop fetching
 * the media resource, after the resource was established to be usable.
 * @const {number}
 */
MediaError.MEDIA_ERR_NETWORK;

/**
 * An error of some description occurred while decoding the media resource,
 * after the resource was established to be usable.
 * @const {number}
 */
MediaError.MEDIA_ERR_DECODE;

/**
 * The media resource indicated by the src attribute was not suitable.
 * @const {number}
 */
MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED;

// HTML5 MessageChannel
/**
 * @see http://dev.w3.org/html5/spec/comms.html#messagechannel
 * @constructor
 */
function MessageChannel() {}

/**
 * Returns the first port.
 * @type {!MessagePort}
 */
MessageChannel.prototype.port1;

/**
 * Returns the second port.
 * @type {!MessagePort}
 */
MessageChannel.prototype.port2;

// HTML5 MessagePort
/**
 * @see http://dev.w3.org/html5/spec/comms.html#messageport
 * @constructor
 * @implements {EventTarget}
 * @implements {Transferable}
 */
function MessagePort() {}

/** @override */
MessagePort.prototype.addEventListener = function(
    type, listener, opt_options) {};

/** @override */
MessagePort.prototype.removeEventListener = function(
    type, listener, opt_options) {};

/** @override */
MessagePort.prototype.dispatchEvent = function(evt) {};


/**
 * Posts a message through the channel, optionally with the given
 * Array of Transferables.
 * @param {*} message
 * @param {Array<!Transferable>=} opt_transfer
 * @return {undefined}
 */
MessagePort.prototype.postMessage = function(message, opt_transfer) {};

/**
 * Begins dispatching messages received on the port.
 * @return {undefined}
 */
MessagePort.prototype.start = function() {};

/**
 * Disconnects the port, so that it is no longer active.
 * @return {undefined}
 */
MessagePort.prototype.close = function() {};

/**
 * TODO(blickly): Change this to MessageEvent<*> and add casts as needed
 * @type {?function(!MessageEvent<?>): void}
 */
MessagePort.prototype.onmessage;

/**
 * @type {?function(!MessageEvent<*>): void}
 * @see https://developer.mozilla.org/docs/Web/API/MessagePort/messageerror_event
 */
MessagePort.prototype.onmessageerror;

// HTML5 MessageEvent class
/**
 * @typedef {Window|MessagePort|ServiceWorker}
 * @see https://html.spec.whatwg.org/multipage/comms.html#messageeventsource
 */
var MessageEventSource;


/**
 * @record
 * @extends {EventInit}
 * @template T
 * @see https://html.spec.whatwg.org/multipage/comms.html#messageeventinit
 */
function MessageEventInit() {}

/** @type {T|undefined} */
MessageEventInit.prototype.data;

/** @type {(string|undefined)} */
MessageEventInit.prototype.origin;

/** @type {(string|undefined)} */
MessageEventInit.prototype.lastEventId;

/** @type {(?MessageEventSource|undefined)} */
MessageEventInit.prototype.source;

/** @type {(!Array<MessagePort>|undefined)} */
MessageEventInit.prototype.ports;


/**
 * @see https://html.spec.whatwg.org/multipage/comms.html#messageevent
 * @constructor
 * @extends {Event}
 * @param {string} type
 * @param {MessageEventInit<T>=} opt_eventInitDict
 * @template T
 */
function MessageEvent(type, opt_eventInitDict) {}

/**
 * The data payload of the message.
 * @type {T}
 */
MessageEvent.prototype.data;

/**
 * The origin of the message, for server-sent events and cross-document
 * messaging.
 * @type {string}
 */
MessageEvent.prototype.origin;

/**
 * The last event ID, for server-sent events.
 * @type {string}
 */
MessageEvent.prototype.lastEventId;

/**
 * The window that dispatched the event.
 * @type {Window}
 */
MessageEvent.prototype.source;

/**
 * The Array of MessagePorts sent with the message, for cross-document
 * messaging and channel messaging.
 * @type {!Array<MessagePort>}
 */
MessageEvent.prototype.ports;

/**
 * Initializes the event in a manner analogous to the similarly-named methods in
 * the DOM Events interfaces.
 * @param {string} typeArg
 * @param {boolean=} canBubbleArg
 * @param {boolean=} cancelableArg
 * @param {T=} dataArg
 * @param {string=} originArg
 * @param {string=} lastEventIdArg
 * @param {?MessageEventSource=} sourceArg
 * @param {!Array<MessagePort>=} portsArg
 * @return {undefined}
 */
MessageEvent.prototype.initMessageEvent = function(
    typeArg, canBubbleArg, cancelableArg, dataArg, originArg, lastEventIdArg,
    sourceArg, portsArg) {};

/**
 * Initializes the event in a manner analogous to the similarly-named methods in
 * the DOM Events interfaces.
 * @param {string} namespaceURI
 * @param {string=} typeArg
 * @param {boolean=} canBubbleArg
 * @param {boolean=} cancelableArg
 * @param {T=} dataArg
 * @param {string=} originArg
 * @param {string=} lastEventIdArg
 * @param {?MessageEventSource=} sourceArg
 * @param {!Array<MessagePort>=} portsArg
 * @return {undefined}
 */
MessageEvent.prototype.initMessageEventNS = function(
    namespaceURI, typeArg, canBubbleArg, cancelableArg, dataArg, originArg,
    lastEventIdArg, sourceArg, portsArg) {};

/**
 * @record
 * @extends {EventInit}
 * @see https://html.spec.whatwg.org/multipage/web-sockets.html#the-closeevent-interface
 */
function CloseEventInit() {}

/**
 * @type {undefined|boolean}
 */
CloseEventInit.prototype.wasClean;

/**
 * @type {undefined|number}
 */
CloseEventInit.prototype.code;

/**
 * @type {undefined|string}
 */
CloseEventInit.prototype.reason;

/**
 * @constructor
 * @extends {Event}
 * @param {string} type
 * @param {!CloseEventInit=} opt_init
 */
var CloseEvent = function(type, opt_init) {};

/**
 * @type {boolean}
 */
CloseEvent.prototype.wasClean;

/**
 * @type {number}
 */
CloseEvent.prototype.code;

/**
 * @type {string}
 */
CloseEvent.prototype.reason;

/**
 * HTML5 BroadcastChannel class.
 * @param {string} channelName
 * @see https://developer.mozilla.org/en-US/docs/Web/API/BroadcastChannel
 * @see https://html.spec.whatwg.org/multipage/comms.html#dom-broadcastchannel
 * @implements {EventTarget}
 * @constructor
 */
function BroadcastChannel(channelName) {}

/**
 * Sends the message, of any type of object, to each BroadcastChannel object
 * listening to the same channel.
 * @param {*} message
 */
BroadcastChannel.prototype.postMessage = function(message) {};

/**
 * Closes the channel object, indicating it won't get any new messages, and
 * allowing it to be, eventually, garbage collected.
 * @return {void}
 */
BroadcastChannel.prototype.close = function() {};

/** @override */
BroadcastChannel.prototype.addEventListener = function(
    type, listener, opt_options) {};

/** @override */
BroadcastChannel.prototype.dispatchEvent = function(evt) {};

/** @override */
BroadcastChannel.prototype.removeEventListener = function(
    type, listener, opt_options) {};

/**
 * An EventHandler property that specifies the function to execute when a
 * message event is fired on this object.
 * @type {?function(!MessageEvent<*>)}
 */
BroadcastChannel.prototype.onmessage;

/**
 * @type {?function(!MessageEvent<*>)}
 * @see https://developer.mozilla.org/docs/Web/API/BroadcastChannel/messageerror_event
 */
BroadcastChannel.prototype.onmessageerror;

/**
 * The name of the channel.
 * @type {string}
 */
BroadcastChannel.prototype.name;

/**
 * @constructor
 */
function AbstractRange() {}

/** @type {boolean} */
AbstractRange.prototype.collapsed;

/** @type {Node} */
AbstractRange .prototype.endContainer;

/** @type {number} */
AbstractRange.prototype.endOffset;

/** @type {Node} */
AbstractRange.prototype.startContainer;

/** @type {number} */
AbstractRange.prototype.startOffset;

/**
 * @typedef {{
 *   endContainer: Node,
 *   endOffset: number,
 *   startContainer: Node,
 *   startOffset: number,
 * }}
 */
var StaticRangeInit;

/**
 * StaticRange class.
 * @constructor
 * @extends {AbstractRange}
 * @param {StaticRangeInit} init
 * @see https://developer.mozilla.org/en-US/docs/Web/API/StaticRange
 */
function StaticRange(init) {}

/**
 * HTML5 DataTransfer class.
 *
 * @see http://www.w3.org/TR/2011/WD-html5-20110113/dnd.html
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/dnd.html
 * @see http://developers.whatwg.org/dnd.html#datatransferitem
 * @constructor
 */
function DataTransfer() {}

/** @type {string} */
DataTransfer.prototype.dropEffect;

/** @type {string} */
DataTransfer.prototype.effectAllowed;

/** @type {!Array<string>} */
DataTransfer.prototype.types;

/** @type {!FileList} */
DataTransfer.prototype.files;

/**
 * @param {string=} opt_format Format for which to remove data.
 * @return {undefined}
 */
DataTransfer.prototype.clearData = function(opt_format) {};

/**
 * @param {string} format Format for which to set data.
 * @param {string} data Data to add.
 * @return {boolean}
 */
DataTransfer.prototype.setData = function(format, data) {};

/**
 * @param {string} format Format for which to set data.
 * @return {string} Data for the given format.
 */
DataTransfer.prototype.getData = function(format) {
  return '';
};

/**
 * @param {HTMLElement} img The image to use when dragging.
 * @param {number} x Horizontal position of the cursor.
 * @param {number} y Vertical position of the cursor.
 * @return {undefined}
 */
DataTransfer.prototype.setDragImage = function(img, x, y) {};

/**
 * @param {HTMLElement} elem Element to receive drag result events.
 * @return {undefined}
 */
DataTransfer.prototype.addElement = function(elem) {};

/**
 * Addition for accessing clipboard file data that are part of the proposed
 * HTML5 spec.
 * @type {DataTransfer}
 */
MouseEvent.prototype.dataTransfer;

/**
 * @record
 * @extends {MouseEventInit}
 * @see https://w3c.github.io/uievents/#idl-wheeleventinit
 */
function WheelEventInit() {}

/** @type {undefined|number} */
WheelEventInit.prototype.deltaX;

/** @type {undefined|number} */
WheelEventInit.prototype.deltaY;

/** @type {undefined|number} */
WheelEventInit.prototype.deltaZ;

/** @type {undefined|number} */
WheelEventInit.prototype.deltaMode;

/**
 * @param {string} type
 * @param {WheelEventInit=} opt_eventInitDict
 * @see http://www.w3.org/TR/DOM-Level-3-Events/#interface-WheelEvent
 * @constructor
 * @extends {MouseEvent}
 */
function WheelEvent(type, opt_eventInitDict) {}

/** @const {number} */
WheelEvent.DOM_DELTA_PIXEL;

/** @const {number} */
WheelEvent.DOM_DELTA_LINE;

/** @const {number} */
WheelEvent.DOM_DELTA_PAGE;

/** @const {number} */
WheelEvent.prototype.deltaX;

/** @const {number} */
WheelEvent.prototype.deltaY;

/** @const {number} */
WheelEvent.prototype.deltaZ;

/** @const {number} */
WheelEvent.prototype.deltaMode;

/**
 * HTML5 DataTransferItem class.
 *
 * @see http://www.w3.org/TR/2011/WD-html5-20110113/dnd.html
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/dnd.html
 * @see http://developers.whatwg.org/dnd.html#datatransferitem
 * @constructor
 */
function DataTransferItem() {}

/** @type {string} */
DataTransferItem.prototype.kind;

/** @type {string} */
DataTransferItem.prototype.type;

/**
 * @param {function(string)} callback
 * @return {undefined}
 */
DataTransferItem.prototype.getAsString = function(callback) {};

/**
 * @return {?File} The file corresponding to this item, or null.
 * @nosideeffects
 */
DataTransferItem.prototype.getAsFile = function() {
  return null;
};

/**
 * HTML5 DataTransferItemList class. There are some discrepancies in the docs
 * on the whatwg.org site. When in doubt, these prototypes match what is
 * implemented as of Chrome 30.
 *
 * @see http://www.w3.org/TR/2011/WD-html5-20110113/dnd.html
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/dnd.html
 * @see http://developers.whatwg.org/dnd.html#datatransferitem
 * @constructor
 * @implements {IArrayLike<!DataTransferItem>}
 */
function DataTransferItemList() {}

/** @type {number} */
DataTransferItemList.prototype.length;

/**
 * @param {number} i File to return from the list.
 * @return {DataTransferItem} The ith DataTransferItem in the list, or null.
 * @nosideeffects
 */
DataTransferItemList.prototype.item = function(i) {
  return null;
};

/**
 * Adds an item to the list.
 * @param {string|!File} data Data for the item being added.
 * @param {string=} opt_type Mime type of the item being added. MUST be present
 *     if the {@code data} parameter is a string.
 * @return {DataTransferItem}
 */
DataTransferItemList.prototype.add = function(data, opt_type) {};

/**
 * Removes an item from the list.
 * @param {number} i File to remove from the list.
 * @return {undefined}
 */
DataTransferItemList.prototype.remove = function(i) {};

/**
 * Removes all items from the list.
 * @return {undefined}
 */
DataTransferItemList.prototype.clear = function() {};

/** @type {!DataTransferItemList} */
DataTransfer.prototype.items;

/**
 * @record
 * @extends {MouseEventInit}
 * @see http://w3c.github.io/html/editing.html#dictdef-drageventinit
 */
function DragEventInit() {}

/** @type {undefined|?DataTransfer} */
DragEventInit.prototype.dataTransfer;


/**
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/dnd.html#the-dragevent-interface
 * @constructor
 * @extends {MouseEvent}
 * @param {string} type
 * @param {DragEventInit=} opt_eventInitDict
 */
function DragEvent(type, opt_eventInitDict) {}

/** @type {DataTransfer} */
DragEvent.prototype.dataTransfer;


/**
 * @record
 * @extends {EventInit}
 * @see https://www.w3.org/TR/progress-events/#progresseventinit
 */
function ProgressEventInit() {}

/** @type {undefined|boolean} */
ProgressEventInit.prototype.lengthComputable;

/** @type {undefined|number} */
ProgressEventInit.prototype.loaded;

/** @type {undefined|number} */
ProgressEventInit.prototype.total;

/**
 * @constructor
 * @template TARGET
 * @param {string} type
 * @param {ProgressEventInit=} opt_progressEventInitDict
 * @extends {Event}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/ProgressEvent
 */
function ProgressEvent(type, opt_progressEventInitDict) {}

/**
 * @override
 * @type {TARGET}
 */
ProgressEvent.prototype.target;

/** @type {number} */
ProgressEvent.prototype.total;

/** @type {number} */
ProgressEvent.prototype.loaded;

/** @type {boolean} */
ProgressEvent.prototype.lengthComputable;


/**
 * @constructor
 */
function TimeRanges() {}

/** @type {number} */
TimeRanges.prototype.length;

/**
 * @param {number} index The index.
 * @return {number} The start time of the range at index.
 * @throws {DOMException}
 */
TimeRanges.prototype.start = function(index) {
  return 0;
};

/**
 * @param {number} index The index.
 * @return {number} The end time of the range at index.
 * @throws {DOMException}
 */
TimeRanges.prototype.end = function(index) {
  return 0;
};


// HTML5 Web Socket class
/**
 * @see https://html.spec.whatwg.org/multipage/web-sockets.html
 * @constructor
 * @param {!URL|string} url
 * @param {(string|!Array<string>)=} opt_protocol
 * @implements {EventTarget}
 */
function WebSocket(url, opt_protocol) {}

/**
 * The connection has not yet been established.
 * @const {number}
 */
WebSocket.CONNECTING;

/**
 * The connection has not yet been established.
 * @const {number}
 */
WebSocket.prototype.CONNECTING;

/**
 * The WebSocket connection is established and communication is possible.
 * @const {number}
 */
WebSocket.OPEN;

/**
 * The WebSocket connection is established and communication is possible.
 * @const {number}
 */
WebSocket.prototype.OPEN;

/**
 * The connection is going through the closing handshake, or the close() method
 * has been invoked.
 * @const {number}
 */
WebSocket.CLOSING;

/**
 * The connection is going through the closing handshake, or the close() method
 * has been invoked.
 * @const {number}
 */
WebSocket.prototype.CLOSING;

/**
 * The connection has been closed or could not be opened.
 * @const {number}
 */
WebSocket.CLOSED;

/**
 * The connection has been closed or could not be opened.
 * @const {number}
 */
WebSocket.prototype.CLOSED;

/** @override */
WebSocket.prototype.addEventListener = function(type, listener, opt_options) {};

/** @override */
WebSocket.prototype.removeEventListener = function(
    type, listener, opt_options) {};

/** @override */
WebSocket.prototype.dispatchEvent = function(evt) {};

/**
 * Returns the URL value that was passed to the constructor.
 * @type {string}
 */
WebSocket.prototype.url;

/**
 * Represents the state of the connection.
 * @type {number}
 */
WebSocket.prototype.readyState;

/**
 * Returns the number of bytes that have been queued but not yet sent.
 * @type {number}
 */
WebSocket.prototype.bufferedAmount;

/**
 * An event handler called on error event.
 * @type {?function(!Event): void}
 */
WebSocket.prototype.onerror;

/**
 * An event handler called on open event.
 * @type {?function(!Event): void}
 */
WebSocket.prototype.onopen;

/**
 * An event handler called on message event.
 * @type {?function(!MessageEvent<string|!ArrayBuffer|!Blob>): void}
 */
WebSocket.prototype.onmessage;

/**
 * An event handler called on close event.
 * @type {?function(!CloseEvent): void}
 */
WebSocket.prototype.onclose;

/**
 * Transmits data using the connection.
 * @param {string|!ArrayBuffer|!ArrayBufferView|!Blob} data
 * @return {void}
 */
WebSocket.prototype.send = function(data) {};

/**
 * Closes the Web Socket connection or connection attempt, if any.
 * @param {number=} opt_code
 * @param {string=} opt_reason
 * @return {undefined}
 */
WebSocket.prototype.close = function(opt_code, opt_reason) {};

/**
 * @type {string} Sets the type of data (blob or arraybuffer) for binary data.
 */
WebSocket.prototype.binaryType;

// HTML5 History
/**
 * @constructor
 * @see http://w3c.github.io/html/browsers.html#the-history-interface
 */
function History() {}

/**
 * Goes back one step in the joint session history.
 * If there is no previous page, does nothing.
 *
 * @see https://html.spec.whatwg.org/multipage/history.html#dom-history-back
 *
 * @return {undefined}
 */
History.prototype.back = function() {};

/**
 * Goes forward one step in the joint session history.
 * If there is no next page, does nothing.
 *
 * @return {undefined}
 */
History.prototype.forward = function() {};

/**
 * The number of entries in the joint session history.
 *
 * @type {number}
 */
History.prototype.length;

/**
 * Goes back or forward the specified number of steps in the joint session
 * history. A zero delta will reload the current page. If the delta is out of
 * range, does nothing.
 *
 * @param {number} delta The number of entries to go back.
 * @return {undefined}
 */
History.prototype.go = function(delta) {};

/**
 * Pushes a new state into the session history.
 * @see http://www.w3.org/TR/html5/history.html#the-history-interface
 * @param {*} data New state.
 * @param {string} title The title for a new session history entry.
 * @param {!URL|string=} url The URL for a new session history entry.
 * @return {undefined}
 */
History.prototype.pushState = function(data, title, url) {};

/**
 * Replaces the current state in the session history.
 * @see http://www.w3.org/TR/html5/history.html#the-history-interface
 * @param {*} data New state.
 * @param {string} title The title for a session history entry.
 * @param {!URL|string=} url The URL for a new session history entry.
 * @return {undefined}
 */
History.prototype.replaceState = function(data, title, url) {};

/**
 * Pending state object.
 * @see https://developer.mozilla.org/en-US/docs/Web/Guide/API/DOM/Manipulating_the_browser_history#Reading_the_current_state
 * @type {*}
 */
History.prototype.state;

/**
 * Allows web applications to explicitly set default scroll restoration behavior
 * on history navigation. This property can be either auto or manual.
 *
 * Non-standard. Only supported in Chrome 46+.
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/API/History
 * @see https://majido.github.io/scroll-restoration-proposal/history-based-api.html
 * @type {string}
 */
History.prototype.scrollRestoration;

/**
 * Add history property to Window.
 *
 * @type {!History}
 */
Window.prototype.history;

/**
 * @constructor
 * @see https://html.spec.whatwg.org/multipage/history.html#the-location-interface
 */
function Location() {}

/**
 * Returns the Location object's URL. Can be set, to navigate to the given URL.
 * @implicitCast
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/history.html#dom-location-href
 */
Location.prototype.href;

/**
 * Returns the Location object's URL's origin.
 * @const {string}
 * @see https://html.spec.whatwg.org/multipage/history.html#dom-location-origin
 */
Location.prototype.origin;

/**
 * Returns the Location object's URL's scheme. Can be set, to navigate to the
 * same URL with a changed scheme.
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/history.html#dom-location-protocol
 */
Location.prototype.protocol;

/**
 * Returns the Location object's URL's host and port (if different from the
 * default port for the scheme). Can be set, to navigate to the same URL with
 * a changed host and port.
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/history.html#dom-location-host
 */
Location.prototype.host;

/**
 * Returns the Location object's URL's host. Can be set, to navigate to the
 * same URL with a changed host.
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/history.html#dom-location-hostname
 */
Location.prototype.hostname;

/**
 * Returns the Location object's URL's port. Can be set, to navigate to the
 * same URL with a changed port.
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/history.html#the-location-interface:dom-location-port
 */
Location.prototype.port;

/**
 * Returns the Location object's URL's path. Can be set, to navigate to the
 * same URL with a changed path.
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/history.html#dom-location-pathname
 */
Location.prototype.pathname;

/**
 * Returns the Location object's URL's query (includes leading "?" if
 * non-empty). Can be set, to navigate to the same URL with a changed query
 * (ignores leading "?").
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/history.html#dom-location-search
 */
Location.prototype.search;

/**
 * Returns the Location object's URL's fragment (includes leading "#" if
 * non-empty). Can be set, to navigate to the same URL with a changed fragment
 * (ignores leading "#").
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/history.html#dom-location-hash
 */
Location.prototype.hash;

/**
 * Navigates to the given page.
 * @param {!URL|string} url
 * @return {undefined}
 * @see https://html.spec.whatwg.org/multipage/history.html#dom-location-assign
 */
Location.prototype.assign = function(url) {};

/**
 * Removes the current page from the session history and navigates to the given
 * page.
 * @param {!URL|string} url
 * @return {undefined}
 * @see https://html.spec.whatwg.org/multipage/history.html#dom-location-replace
 */
Location.prototype.replace = function(url) {};

/**
 * Reloads the current page.
 * @param {boolean=} forceReload If true, reloads the page from
 *     the server. Defaults to false.
 * @return {undefined}
 * @see https://html.spec.whatwg.org/multipage/history.html#dom-location-reload
 */
Location.prototype.reload = function(forceReload) {};

/**
 * Returns a DOMStringList object listing the origins of the ancestor browsing
 * contexts, from the parent browsing context to the top-level browsing
 * context.
 * @type {DOMStringList}
 * @see https://html.spec.whatwg.org/multipage/history.html#dom-location-ancestororigins
 */
Location.prototype.ancestorOrigins;

/**
 * @see http://www.whatwg.org/specs/web-apps/current-work/#popstateevent
 * @constructor
 * @extends {Event}
 *
 * @param {string} type
 * @param {{state: *}=} opt_eventInitDict
 */
function PopStateEvent(type, opt_eventInitDict) {}

/**
 * @type {*}
 */
PopStateEvent.prototype.state;

/**
 * Initializes the event after it has been created with document.createEvent
 * @param {string} typeArg
 * @param {boolean} canBubbleArg
 * @param {boolean} cancelableArg
 * @param {*} stateArg
 * @return {undefined}
 */
PopStateEvent.prototype.initPopStateEvent = function(
    typeArg, canBubbleArg, cancelableArg, stateArg) {};

/**
 * Returns true if the user agent performed a visual transition for this
 * navigation before dispatching this event. iOS edge swipe back buttons are
 * captured using this property.
 * As of 2025-02-18, this is not fully supported by all Tier 1 browsers.
 * See https://developer.mozilla.org/en-US/docs/Web/API/PopStateEvent/hasUAVisualTransition
 * @type {boolean}
 */
PopStateEvent.prototype.hasUAVisualTransition;

/**
 * @see http://www.whatwg.org/specs/web-apps/current-work/#hashchangeevent
 * @constructor
 * @extends {Event}
 *
 * @param {string} type
 * @param {{oldURL: string, newURL: string}=} opt_eventInitDict
 */
function HashChangeEvent(type, opt_eventInitDict) {}

/** @type {string} */
HashChangeEvent.prototype.oldURL;

/** @type {string} */
HashChangeEvent.prototype.newURL;

/**
 * Initializes the event after it has been created with document.createEvent
 * @param {string} typeArg
 * @param {boolean} canBubbleArg
 * @param {boolean} cancelableArg
 * @param {string} oldURLArg
 * @param {string} newURLArg
 * @return {undefined}
 */
HashChangeEvent.prototype.initHashChangeEvent = function(
    typeArg, canBubbleArg, cancelableArg, oldURLArg, newURLArg) {};

/**
 * @see http://www.whatwg.org/specs/web-apps/current-work/#pagetransitionevent
 * @constructor
 * @extends {Event}
 *
 * @param {string} type
 * @param {{persisted: boolean}=} opt_eventInitDict
 */
function PageTransitionEvent(type, opt_eventInitDict) {}

/** @type {boolean} */
PageTransitionEvent.prototype.persisted;

/**
 * Initializes the event after it has been created with document.createEvent
 * @param {string} typeArg
 * @param {boolean} canBubbleArg
 * @param {boolean} cancelableArg
 * @param {*} persistedArg
 * @return {undefined}
 */
PageTransitionEvent.prototype.initPageTransitionEvent = function(
    typeArg, canBubbleArg, cancelableArg, persistedArg) {};

/**
 * @constructor
 * @implements {IArrayLike<!File>}
 */
function FileList() {}

/** @type {number} */
FileList.prototype.length;

/**
 * @param {number} i File to return from the list.
 * @return {File} The ith file in the list.
 * @nosideeffects
 */
FileList.prototype.item = function(i) {
  return null;
};

/**
 * @type {boolean}
 * @see http://dev.w3.org/2006/webapi/XMLHttpRequest-2/#withcredentials
 */
XMLHttpRequest.prototype.withCredentials;

/**
 * @type {?function(!ProgressEvent): void}
 * @see https://xhr.spec.whatwg.org/#handler-xhr-onloadstart
 */
XMLHttpRequest.prototype.onloadstart;

/**
 * @type {?function(!ProgressEvent): void}
 * @see https://dvcs.w3.org/hg/xhr/raw-file/tip/Overview.html#handler-xhr-onprogress
 */
XMLHttpRequest.prototype.onprogress;

/**
 * @type {?function(!ProgressEvent): void}
 * @see https://xhr.spec.whatwg.org/#handler-xhr-onabort
 */
XMLHttpRequest.prototype.onabort;

/**
 * @type {?function(!ProgressEvent): void}
 * @see https://xhr.spec.whatwg.org/#handler-xhr-onload
 */
XMLHttpRequest.prototype.onload;

/**
 * @type {?function(!ProgressEvent): void}
 * @see https://xhr.spec.whatwg.org/#handler-xhr-ontimeout
 */
XMLHttpRequest.prototype.ontimeout;

/**
 * @type {?function(!ProgressEvent): void}
 * @see https://xhr.spec.whatwg.org/#handler-xhr-onloadend
 */
XMLHttpRequest.prototype.onloadend;

/**
 * @type {XMLHttpRequestUpload}
 * @see http://dev.w3.org/2006/webapi/XMLHttpRequest-2/#the-upload-attribute
 */
XMLHttpRequest.prototype.upload;

/**
 * @param {string} mimeType The mime type to override with.
 * @return {undefined}
 */
XMLHttpRequest.prototype.overrideMimeType = function(mimeType) {};

/**
 * @type {string}
 * @see http://dev.w3.org/2006/webapi/XMLHttpRequest-2/#the-responsetype-attribute
 */
XMLHttpRequest.prototype.responseType;

/**
 * @type {?(ArrayBuffer|Blob|Document|Object|string)}
 * @see http://dev.w3.org/2006/webapi/XMLHttpRequest-2/#the-response-attribute
 */
XMLHttpRequest.prototype.response;


/**
 * @type {ArrayBuffer}
 * Implemented as a draft spec in Firefox 4 as the way to get a requested array
 * buffer from an XMLHttpRequest.
 * @see https://developer.mozilla.org/En/Using_XMLHttpRequest#Receiving_binary_data_using_JavaScript_typed_arrays
 *
 * This property is not used anymore and should be removed.
 * @see https://github.com/google/closure-compiler/pull/1389
 */
XMLHttpRequest.prototype.mozResponseArrayBuffer;

/**
 * XMLHttpRequestEventTarget defines events for checking the status of a data
 * transfer between a client and a server. This should be a common base class
 * for XMLHttpRequest and XMLHttpRequestUpload.
 *
 * @constructor
 * @implements {EventTarget}
 */
function XMLHttpRequestEventTarget() {}

/** @override */
XMLHttpRequestEventTarget.prototype.addEventListener = function(
    type, listener, opt_options) {};

/** @override */
XMLHttpRequestEventTarget.prototype.removeEventListener = function(
    type, listener, opt_options) {};

/** @override */
XMLHttpRequestEventTarget.prototype.dispatchEvent = function(evt) {};

/**
 * An event target to track the status of an upload.
 *
 * @constructor
 * @extends {XMLHttpRequestEventTarget}
 */
function XMLHttpRequestUpload() {}

/**
 * @type {?function(!ProgressEvent): void}
 * @see https://dvcs.w3.org/hg/xhr/raw-file/tip/Overview.html#handler-xhr-onprogress
 */
XMLHttpRequestUpload.prototype.onprogress;

/**
 * @param {number=} opt_width
 * @param {number=} opt_height
 * @constructor
 * @extends {HTMLImageElement}
 */
function Image(opt_width, opt_height) {}


/**
 * Dataset collection.
 * This is really a DOMStringMap but it behaves close enough to an object to
 * pass as an object.
 * @const {!Object<string, string>}
 */
HTMLElement.prototype.dataset;


/**
 * @constructor
 * @implements {IArrayLike<string>}
 * @see https://dom.spec.whatwg.org/#interface-domtokenlist
 */
function DOMTokenList() {}

/**
 * Returns the number of CSS classes applied to this Element.
 * @type {number}
 */
DOMTokenList.prototype.length;

/**
 * Returns the string value applied to this Element.
 * @type {string|undefined}
 */
DOMTokenList.prototype.value;

/**
 * @param {number} index The index of the item to return.
 * @return {string} The CSS class at the specified index.
 * @nosideeffects
 */
DOMTokenList.prototype.item = function(index) {};

/**
 * @param {string} token The CSS class to check for.
 * @return {boolean} Whether the CSS class has been applied to the Element.
 * @nosideeffects
 */
DOMTokenList.prototype.contains = function(token) {};

/**
 * @param {...string} var_args The CSS class(es) to add to this element.
 * @return {undefined}
 */
DOMTokenList.prototype.add = function(var_args) {};

/**
 * @param {...string} var_args The CSS class(es) to remove from this element.
 * @return {undefined}
 */
DOMTokenList.prototype.remove = function(var_args) {};

/**
 * Replaces token with newToken.
 * @param {string} token The CSS class to replace.
 * @param {string} newToken The new CSS class to use.
 * @return {undefined}
 */
DOMTokenList.prototype.replace = function(token, newToken) {};

/**
 * @param {string} token The token to query for.
 * @return {boolean} Whether the token was found.
 * @see https://developer.mozilla.org/en-US/docs/Web/API/DOMTokenList/supports
 * @nosideeffects
 */
DOMTokenList.prototype.supports = function(token) {};

/**
 * @param {string} token The CSS class to toggle from this element.
 * @param {boolean=} opt_force True to add the class whether it exists
 *     or not. False to remove the class whether it exists or not.
 *     This argument is not supported on IE 10 and below, according to
 *     the MDN page linked below.
 * @return {boolean} False if the token was removed; True otherwise.
 * @see https://developer.mozilla.org/en-US/docs/Web/API/Element.classList
 */
DOMTokenList.prototype.toggle = function(token, opt_force) {};

/**
 * @return {string} A stringified representation of CSS classes.
 * @nosideeffects
 * @override
 */
DOMTokenList.prototype.toString = function() {};

/**
 * @return {!IteratorIterable<string>} An iterator to go through all values of
 *     the key/value pairs contained in this object.
 * @nosideeffects
 * @see https://developer.mozilla.org/en-US/docs/Web/API/DOMTokenList/values
 */
DOMTokenList.prototype.values = function() {};

/**
 * A better interface to CSS classes than className.
 * @const {!DOMTokenList}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/Element/classList
 */
Element.prototype.classList;

/**
 * Requests to submit the form.
 * @param {?HTMLElement=} submitter Submit button, whose attributes can impact
 *     submission.
 * @see https://html.spec.whatwg.org/multipage/forms.html#dom-form-requestsubmit-dev
 */
HTMLFormElement.prototype.requestSubmit = function(submitter) {};

/**
 * Constraint Validation API properties and methods
 * @see http://www.w3.org/TR/2009/WD-html5-20090423/forms.html#the-constraint-validation-api
 */

/** @return {boolean} */
HTMLFormElement.prototype.checkValidity = function() {};

/** @return {boolean} */
HTMLFormElement.prototype.reportValidity = function() {};

/** @type {boolean} */
HTMLFormElement.prototype.noValidate;

/** @constructor */
function ValidityState() {}

/** @type {boolean} */
ValidityState.prototype.badInput;

/** @type {boolean} */
ValidityState.prototype.customError;

/** @type {boolean} */
ValidityState.prototype.patternMismatch;

/** @type {boolean} */
ValidityState.prototype.rangeOverflow;

/** @type {boolean} */
ValidityState.prototype.rangeUnderflow;

/** @type {boolean} */
ValidityState.prototype.stepMismatch;

/** @type {boolean} */
ValidityState.prototype.typeMismatch;

/** @type {boolean} */
ValidityState.prototype.tooLong;

/** @type {boolean} */
ValidityState.prototype.tooShort;

/** @type {boolean} */
ValidityState.prototype.valid;

/** @type {boolean} */
ValidityState.prototype.valueMissing;


/** @type {boolean} */
HTMLButtonElement.prototype.autofocus;

/**
 * Can return null when hidden.
 * See https://html.spec.whatwg.org/multipage/forms.html#dom-lfe-labels
 * @const {?NodeList<!HTMLLabelElement>}
 */
HTMLButtonElement.prototype.labels;

/** @type {string} */
HTMLButtonElement.prototype.validationMessage;

/**
 * @const {ValidityState}
 */
HTMLButtonElement.prototype.validity;

/** @type {boolean} */
HTMLButtonElement.prototype.willValidate;

/** @return {boolean} */
HTMLButtonElement.prototype.checkValidity = function() {};

/** @return {boolean} */
HTMLButtonElement.prototype.reportValidity = function() {};

/**
 * @param {string} message
 * @return {undefined}
 */
HTMLButtonElement.prototype.setCustomValidity = function(message) {};

/**
 * @type {string}
 * @implicitCast
 * @see http://www.w3.org/TR/html5/forms.html#attr-fs-formaction
 */
HTMLButtonElement.prototype.formAction;

/**
 * @type {string}
 * @see http://www.w3.org/TR/html5/forms.html#attr-fs-formenctype
 */
HTMLButtonElement.prototype.formEnctype;

/**
 * @type {string}
 * @see http://www.w3.org/TR/html5/forms.html#attr-fs-formmethod
 */
HTMLButtonElement.prototype.formMethod;

/**
 * @type {string}
 * @see http://www.w3.org/TR/html5/forms.html#attr-fs-formtarget
 */
HTMLButtonElement.prototype.formTarget;

/** @type {boolean} */
HTMLInputElement.prototype.autofocus;

/** @type {boolean} */
HTMLInputElement.prototype.formNoValidate;

/**
 * @type {string}
 * @implicitCast
 * @see http://www.w3.org/TR/html5/forms.html#attr-fs-formaction
 */
HTMLInputElement.prototype.formAction;

/**
 * @type {string}
 * @see http://www.w3.org/TR/html5/forms.html#attr-fs-formenctype
 */
HTMLInputElement.prototype.formEnctype;

/**
 * @type {string}
 * @see http://www.w3.org/TR/html5/forms.html#attr-fs-formmethod
 */
HTMLInputElement.prototype.formMethod;

/**
 * @type {string}
 * @see http://www.w3.org/TR/html5/forms.html#attr-fs-formtarget
 */
HTMLInputElement.prototype.formTarget;

/**
 * Can return null when hidden.
 * See https://html.spec.whatwg.org/multipage/forms.html#dom-lfe-labels
 * @const {?NodeList<!HTMLLabelElement>}
 */
HTMLInputElement.prototype.labels;

/** @type {string} */
HTMLInputElement.prototype.validationMessage;

/**
 * @type {number}
 * @implicitCast
 */
HTMLInputElement.prototype.selectionStart;

/**
 * @type {number}
 * @implicitCast
 */
HTMLInputElement.prototype.selectionEnd;

/** @type {string} */
HTMLInputElement.prototype.selectionDirection;

/**
 * @param {number} start
 * @param {number} end
 * @param {string=} direction
 * @see https://html.spec.whatwg.org/#dom-textarea/input-setselectionrange
 * @return {undefined}
 */
HTMLInputElement.prototype.setSelectionRange = function(
    start, end, direction) {};

/**
 * @param {string} replacement
 * @param {number=} start
 * @param {number=} end
 * @param {string=} selectionMode
 * @see https://html.spec.whatwg.org/#dom-textarea/input-setrangetext
 * @return {undefined}
 */
HTMLInputElement.prototype.setRangeText = function(
    replacement, start, end, selectionMode) {};

/**
 * @const {ValidityState}
 */
HTMLInputElement.prototype.validity;

/** @type {boolean} */
HTMLInputElement.prototype.willValidate;

/** @return {boolean} */
HTMLInputElement.prototype.checkValidity = function() {};

/** @return {boolean} */
HTMLInputElement.prototype.reportValidity = function() {};

/**
 * @param {string} message
 * @return {undefined}
 */
HTMLInputElement.prototype.setCustomValidity = function(message) {};

/** @type {Element} */
HTMLLabelElement.prototype.control;

/** @type {boolean} */
HTMLSelectElement.prototype.autofocus;

/**
 * Can return null when hidden.
 * See https://html.spec.whatwg.org/multipage/forms.html#dom-lfe-labels
 * @const {?NodeList<!HTMLLabelElement>}
 */
HTMLSelectElement.prototype.labels;

/** @type {boolean} */
HTMLSelectElement.prototype.required;

/** @type {HTMLCollection<!HTMLOptionElement>} */
HTMLSelectElement.prototype.selectedOptions;

/** @type {string} */
HTMLSelectElement.prototype.validationMessage;

/**
 * @const {ValidityState}
 */
HTMLSelectElement.prototype.validity;

/** @type {boolean} */
HTMLSelectElement.prototype.willValidate;

/** @return {boolean} */
HTMLSelectElement.prototype.checkValidity = function() {};

/** @return {boolean} */
HTMLSelectElement.prototype.reportValidity = function() {};

/**
 * @param {string} message
 * @return {undefined}
 */
HTMLSelectElement.prototype.setCustomValidity = function(message) {};

/**
 * @constructor
 * @extends {HTMLElement}
 * @see https://html.spec.whatwg.org/#htmlspanelement
 */
function HTMLSpanElement() {}

/** @type {boolean} */
HTMLTextAreaElement.prototype.autofocus;

/**
 * Can return null when hidden.
 * See https://html.spec.whatwg.org/multipage/forms.html#dom-lfe-labels
 * @const {?NodeList<!HTMLLabelElement>}
 */
HTMLTextAreaElement.prototype.labels;

/** @type {number} */
HTMLTextAreaElement.prototype.maxLength;

/** @type {number} */
HTMLTextAreaElement.prototype.minLength;

/** @type {string} */
HTMLTextAreaElement.prototype.placeholder;

/**
 * @type {number}
 * @see https://html.spec.whatwg.org/#dom-textarea/input-selectionstart
 */
HTMLTextAreaElement.prototype.selectionStart;

/**
 * @type {number}
 * @see https://html.spec.whatwg.org/#dom-textarea/input-selectionend
 */
HTMLTextAreaElement.prototype.selectionEnd;

/** @type {number} */
HTMLTextAreaElement.prototype.textLength;

/** @type {string} */
HTMLTextAreaElement.prototype.validationMessage;

/**
 * @const {ValidityState}
 */
HTMLTextAreaElement.prototype.validity;

/** @type {boolean} */
HTMLTextAreaElement.prototype.willValidate;

/** @return {boolean} */
HTMLTextAreaElement.prototype.checkValidity = function() {};

/** @return {boolean} */
HTMLTextAreaElement.prototype.reportValidity = function() {};

/**
 * @param {string} message
 * @return {undefined}
 */
HTMLTextAreaElement.prototype.setCustomValidity = function(message) {};

/**
 * @param {number} selectionStart
 * @param {number} selectionEnd
 * @see http://www.whatwg.org/specs/web-apps/current-work/multipage/editing.html#dom-textarea/input-setselectionrange
 * @return {undefined}
 */
HTMLTextAreaElement.prototype.setSelectionRange = function(
    selectionStart, selectionEnd) {};

/**
 * @param {string} replacement
 * @param {number=} start
 * @param {number=} end
 * @param {string=} selectionMode
 * @see https://html.spec.whatwg.org/#dom-textarea/input-setrangetext
 * @return {undefined}
 */
HTMLTextAreaElement.prototype.setRangeText = function(
    replacement, start, end, selectionMode) {};



/**
 * @constructor
 * @extends {HTMLElement}
 * @see http://www.w3.org/TR/html5/the-embed-element.html#htmlembedelement
 */
function HTMLEmbedElement() {}

/**
 * @type {string}
 * @see http://www.w3.org/TR/html5/dimension-attributes.html#dom-dim-width
 */
HTMLEmbedElement.prototype.width;

/**
 * @type {string}
 * @see http://www.w3.org/TR/html5/dimension-attributes.html#dom-dim-height
 */
HTMLEmbedElement.prototype.height;

/**
 * @type {string}
 * @implicitCast
 * @see http://www.w3.org/TR/html5/the-embed-element.html#dom-embed-src
 */
HTMLEmbedElement.prototype.src;

/**
 * @type {string}
 * @see http://www.w3.org/TR/html5/the-embed-element.html#dom-embed-type
 */
HTMLEmbedElement.prototype.type;

/**
 * @return {?Document}
 * @see https://developer.mozilla.org/docs/Web/API/HTMLEmbedElement/getSVGDocument
 */
HTMLEmbedElement.prototype.getSVGDocument = function() {};

// Fullscreen APIs.

/**
 * @record
 * @see https://fullscreen.spec.whatwg.org/#dictdef-fullscreenoptions
 */
function FullscreenOptions() {}

/** @type {string} */
FullscreenOptions.prototype.navigationUI;

/**
 * @see https://fullscreen.spec.whatwg.org/#dom-element-requestfullscreen
 * @param {!FullscreenOptions=} options
 * @return {!Promise<undefined>}
 */
Element.prototype.requestFullscreen = function(options) {};

/**
 * @type {string}
 * @see https://dom.spec.whatwg.org/#dom-document-characterset
 */
Document.prototype.characterSet;

/**
 * @type {string}
 * @see https://dom.spec.whatwg.org/#dom-document-contenttype
 */
Document.prototype.contentType;

/**
 * @see https://dom.spec.whatwg.org/#dom-document-compatmode
 * @type {string}
 */
Document.prototype.compatMode;

/**
 * @see https://html.spec.whatwg.org/multipage/nav-history-apis.html#dom-document-defaultview-dev
 * @type {?Window}
 */
Document.prototype.defaultView;

/**
 * @see https://html.spec.whatwg.org/multipage/interaction.html#dom-document-designmode-dev
 * @type {string}
 */
Document.prototype.designMode;

/**
 * @type {boolean}
 * @see http://www.w3.org/TR/2012/WD-fullscreen-20120703/#dom-document-fullscreenenabled
 */
Document.prototype.fullscreenEnabled;

/**
 * @type {boolean}
 * @see https://developer.mozilla.org/docs/Web/API/Document/fullscreen
 */
Document.prototype.fullscreen;

/**
 * @type {Element}
 * @see http://www.w3.org/TR/2012/WD-fullscreen-20120703/#dom-document-fullscreenelement
 */
Document.prototype.fullscreenElement;

/**
 * @see http://www.w3.org/TR/2012/WD-fullscreen-20120703/#dom-document-exitfullscreen
 * @return {undefined}
 */
Document.prototype.exitFullscreen = function() {};

// Externs definitions of browser current implementations.
// Firefox 10 implementation.
Element.prototype.mozRequestFullScreen = function() {};

Element.prototype.mozRequestFullScreenWithKeys = function() {};

/** @type {boolean} */
Document.prototype.mozFullScreen;

Document.prototype.mozCancelFullScreen = function() {};

/** @type {Element} */
Document.prototype.mozFullScreenElement;

/** @type {boolean} */
Document.prototype.mozFullScreenEnabled;

// Chrome 21 implementation.
/**
 * The current fullscreen element for the document is set to this element.
 * Valid only for Webkit browsers.
 * @param {number=} opt_allowKeyboardInput Whether keyboard input is desired.
 *     Should use ALLOW_KEYBOARD_INPUT constant.
 * @return {undefined}
 */
Element.prototype.webkitRequestFullScreen = function(opt_allowKeyboardInput) {};

/**
 * The current fullscreen element for the document is set to this element.
 * Valid only for Webkit browsers.
 * @param {number=} opt_allowKeyboardInput Whether keyboard input is desired.
 *     Should use ALLOW_KEYBOARD_INPUT constant.
 * @return {undefined}
 */
Element.prototype.webkitRequestFullscreen = function(opt_allowKeyboardInput) {};

/** @type {boolean} */
Document.prototype.webkitIsFullScreen;

Document.prototype.webkitCancelFullScreen = function() {};

/** @type {boolean} */
Document.prototype.webkitFullscreenEnabled;

/** @type {Element} */
Document.prototype.webkitCurrentFullScreenElement;

/** @type {Element} */
Document.prototype.webkitFullscreenElement;

/** @type {boolean} */
Document.prototype.webkitFullScreenKeyboardInputAllowed;

// IE 11 implementation.
// http://msdn.microsoft.com/en-us/library/ie/dn265028(v=vs.85).aspx
/** @return {void} */
Element.prototype.msRequestFullscreen = function() {};

/** @return {void} */
Document.prototype.msExitFullscreen = function() {};

/** @type {boolean} */
Document.prototype.msFullscreenEnabled;

/** @type {Element} */
Document.prototype.msFullscreenElement;

/** @const {number} */
Element.ALLOW_KEYBOARD_INPUT;

/** @const {number} */
Element.prototype.ALLOW_KEYBOARD_INPUT;


/**
 * @typedef {{
 *   childList: (boolean|undefined),
 *   attributes: (boolean|undefined),
 *   characterData: (boolean|undefined),
 *   subtree: (boolean|undefined),
 *   attributeOldValue: (boolean|undefined),
 *   characterDataOldValue: (boolean|undefined),
 *   attributeFilter: (!Array<string>|undefined)
 * }}
 */
var MutationObserverInit;


/** @constructor */
function MutationRecord() {}

/** @type {string} */
MutationRecord.prototype.type;

/** @type {Node} */
MutationRecord.prototype.target;

/** @type {!NodeList<!Node>} */
MutationRecord.prototype.addedNodes;

/** @type {!NodeList<!Node>} */
MutationRecord.prototype.removedNodes;

/** @type {?Node} */
MutationRecord.prototype.previousSibling;

/** @type {?Node} */
MutationRecord.prototype.nextSibling;

/** @type {?string} */
MutationRecord.prototype.attributeName;

/** @type {?string} */
MutationRecord.prototype.attributeNamespace;

/** @type {?string} */
MutationRecord.prototype.oldValue;


/**
 * @see http://www.w3.org/TR/domcore/#mutation-observers
 * @param {function(!Array<!MutationRecord>, !MutationObserver)} callback
 * @constructor
 */
function MutationObserver(callback) {}

/**
 * @param {Node} target
 * @param {MutationObserverInit=} options
 * @return {undefined}
 */
MutationObserver.prototype.observe = function(target, options) {};

MutationObserver.prototype.disconnect = function() {};

/**
 * @return {!Array<!MutationRecord>}
 */
MutationObserver.prototype.takeRecords = function() {};

/**
 * @type {function(new:MutationObserver, function(Array<MutationRecord>))}
 */
Window.prototype.WebKitMutationObserver;

/**
 * @type {function(new:MutationObserver, function(Array<MutationRecord>))}
 */
Window.prototype.MozMutationObserver;


/**
 * @see http://www.w3.org/TR/page-visibility/
 * @type {VisibilityState}
 */
Document.prototype.visibilityState;

/**
 * @type {string}
 */
Document.prototype.mozVisibilityState;

/**
 * @type {string}
 */
Document.prototype.webkitVisibilityState;

/**
 * @type {string}
 */
Document.prototype.msVisibilityState;

/**
 * @see http://www.w3.org/TR/page-visibility/
 * @type {boolean}
 */
Document.prototype.hidden;

/**
 * @type {boolean}
 */
Document.prototype.mozHidden;

/**
 * @type {boolean}
 */
Document.prototype.webkitHidden;

/**
 * @type {boolean}
 */
Document.prototype.msHidden;

/**
 * @see http://www.w3.org/TR/components-intro/
 * @see http://w3c.github.io/webcomponents/spec/custom/#extensions-to-document-interface-to-register
 * @param {string} type
 * @param {{extends: (string|undefined), prototype: (Object|undefined)}=}
 *     options
 * @return {function(new:Element, ...*)} a constructor for the new tag.
 * @deprecated document.registerElement() is deprecated in favor of
 *     customElements.define()
 */
Document.prototype.registerElement = function(type, options) {};

/**
 * @see http://www.w3.org/TR/components-intro/
 * @see http://w3c.github.io/webcomponents/spec/custom/#extensions-to-document-interface-to-register
 * @param {string} type
 * @param {{extends: (string|undefined), prototype: (Object|undefined)}} options
 * @deprecated This method has been removed and will be removed soon from this
 *     file.
 */
Document.prototype.register = function(type, options) {};

/**
 * @type {!FontFaceSet}
 * @see http://dev.w3.org/csswg/css-font-loading/#dom-fontfacesource-fonts
 */
Document.prototype.fonts;


/**
 * @type {?HTMLScriptElement}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/Document/currentScript
 */
Document.prototype.currentScript;

/**
 * @see https://wicg.github.io/nav-speculation/prerendering.html#dom-document-prerendering
 * @type {boolean}
 */
Document.prototype.prerendering;

/**
 * Definition of ShadowRoot interface,
 * @see http://www.w3.org/TR/shadow-dom/#api-shadow-root
 * @constructor
 * @extends {DocumentFragment}
 */
function ShadowRoot() {}

/**
 * The host element that a ShadowRoot is attached to.
 * Note: this is not yet W3C standard but is undergoing development.
 * W3C feature tracking bug:
 * https://www.w3.org/Bugs/Public/show_bug.cgi?id=22399
 * Draft specification:
 * https://dvcs.w3.org/hg/webcomponents/raw-file/6743f1ace623/spec/shadow/index.html#shadow-root-object
 * @type {!Element}
 */
ShadowRoot.prototype.host;

/**
 * @param {string} id id.
 * @return {HTMLElement}
 * @nosideeffects
 */
ShadowRoot.prototype.getElementById = function(id) {};


/**
 * @return {Selection}
 * @nosideeffects
 */
ShadowRoot.prototype.getSelection = function() {};


/**
 * @param {number} x
 * @param {number} y
 * @return {Element}
 * @nosideeffects
 */
ShadowRoot.prototype.elementFromPoint = function(x, y) {};


/**
 * @param {number} x
 * @param {number} y
 * @return {!IArrayLike<!Element>}
 * @nosideeffects
 */
ShadowRoot.prototype.elementsFromPoint = function(x, y) {};


/**
 * @type {?Element}
 */
ShadowRoot.prototype.activeElement;


/**
 * @type {!ShadowRootMode}
 */
ShadowRoot.prototype.mode;


/**
 * @type {?ShadowRoot}
 * @deprecated
 */
ShadowRoot.prototype.olderShadowRoot;


/**
 * @type {string}
 * @implicitCast
 */
ShadowRoot.prototype.innerHTML;


/**
 * @type {!StyleSheetList}
 */
ShadowRoot.prototype.styleSheets;


/**
 * @param {!GetHTMLOptions=} options
 * @return {string}
 * @see https://developer.mozilla.org/docs/Web/API/ShadowRoot/getHTML
 */
ShadowRoot.prototype.getHTML = function(options) {};

/** @type {boolean} */
ShadowRoot.prototype.clonable;

/** @type {boolean} */
ShadowRoot.prototype.serializable;

/**
 * @param {string} html
 * @return {undefined}
 */
ShadowRoot.prototype.setHTMLUnsafe = function(html) {};

/**
 * @typedef {string}
 * @see https://dom.spec.whatwg.org/#enumdef-shadowrootmode
 */
var ShadowRootMode;


/**
 * @typedef {string}
 * @see https://dom.spec.whatwg.org/#enumdef-slotassignmentmode
 */
var SlotAssignmentMode;


/**
 * @record
 * @see https://dom.spec.whatwg.org/#dictdef-shadowrootinit
 */
function ShadowRootInit() {}

/** @type {!ShadowRootMode} */
ShadowRootInit.prototype.mode;

/** @type {(undefined|boolean)} */
ShadowRootInit.prototype.delegatesFocus;

/** @type {(undefined|SlotAssignmentMode)} */
ShadowRootInit.prototype.slotAssignment;

/** @type {(boolean|undefined)} */
ShadowRootInit.prototype.serializable;

/**
 * @see http://www.w3.org/TR/shadow-dom/#the-content-element
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLContentElement() {}

/**
 * @type {string}
 */
HTMLContentElement.prototype.select;

/**
 * @return {!NodeList<!Node>}
 */
HTMLContentElement.prototype.getDistributedNodes = function() {};


/**
 * @see http://www.w3.org/TR/shadow-dom/#the-shadow-element
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLShadowElement() {}

/**
 * @return {!NodeList<!Node>}
 */
HTMLShadowElement.prototype.getDistributedNodes = function() {};


/**
 * @see http://www.w3.org/TR/html5/webappapis.html#the-errorevent-interface
 *
 * @constructor
 * @extends {Event}
 *
 * @param {string} type
 * @param {ErrorEventInit=} opt_eventInitDict
 */
function ErrorEvent(type, opt_eventInitDict) {}

/** @const {string} */
ErrorEvent.prototype.message;

/** @const {string} */
ErrorEvent.prototype.filename;

/** @const {number} */
ErrorEvent.prototype.lineno;

/** @const {number} */
ErrorEvent.prototype.colno;

/** @const {*} */
ErrorEvent.prototype.error;


/**
 * @record
 * @extends {EventInit}
 * @see https://www.w3.org/TR/html5/webappapis.html#erroreventinit
 */
function ErrorEventInit() {}

/** @type {undefined|string} */
ErrorEventInit.prototype.message;

/** @type {undefined|string} */
ErrorEventInit.prototype.filename;

/** @type {undefined|number} */
ErrorEventInit.prototype.lineno;

/** @type {undefined|number} */
ErrorEventInit.prototype.colno;

/** @type {*} */
ErrorEventInit.prototype.error;


/**
 * @see http://dom.spec.whatwg.org/#dom-domimplementation-createhtmldocument
 * @param {string=} opt_title A title to give the new HTML document
 * @return {!HTMLDocument}
 */
DOMImplementation.prototype.createHTMLDocument = function(opt_title) {};



/**
 * @constructor
 * @see https://html.spec.whatwg.org/multipage/embedded-content.html#the-picture-element
 * @extends {HTMLElement}
 */
function HTMLPictureElement() {}

/**
 * @constructor
 * @see https://html.spec.whatwg.org/multipage/embedded-content.html#the-picture-element
 * @extends {HTMLElement}
 */
function HTMLSourceElement() {}

/** @type {string} */
HTMLSourceElement.prototype.media;

/** @type {string} */
HTMLSourceElement.prototype.sizes;

/** @type {string} @implicitCast */
HTMLSourceElement.prototype.src;

/** @type {string} */
HTMLSourceElement.prototype.srcset;

/** @type {string} */
HTMLSourceElement.prototype.type;

/** @type {string} */
HTMLImageElement.prototype.sizes;

/** @type {string} */
HTMLImageElement.prototype.srcset;


/**
 * 4.11 Interactive elements
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html
 */

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#the-details-element
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLDetailsElement() {}

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-details-open
 * @type {boolean}
 */
HTMLDetailsElement.prototype.open;


// As of 2/20/2015, <summary> has no special web IDL interface nor global
// constructor (i.e. HTMLSummaryElement).


/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-menu-type
 * @type {string}
 */
HTMLMenuElement.prototype.type;

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-menu-label
 * @type {string}
 */
HTMLMenuElement.prototype.label;


/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#the-menuitem-element
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLMenuItemElement() {}

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-menuitem-type
 * @type {string}
 */
HTMLMenuItemElement.prototype.type;

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-menuitem-label
 * @type {string}
 */
HTMLMenuItemElement.prototype.label;

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-menuitem-icon
 * @type {string}
 */
HTMLMenuItemElement.prototype.icon;

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-menuitem-disabled
 * @type {boolean}
 */
HTMLMenuItemElement.prototype.disabled;

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-menuitem-checked
 * @type {boolean}
 */
HTMLMenuItemElement.prototype.checked;

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-menuitem-radiogroup
 * @type {string}
 */
HTMLMenuItemElement.prototype.radiogroup;

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-menuitem-default
 * @type {boolean}
 */
HTMLMenuItemElement.prototype.default;

// TODO(dbeam): add HTMLMenuItemElement.prototype.command if it's implemented.


/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#relatedevent
 * @param {string} type
 * @param {{relatedTarget: (EventTarget|undefined)}=} opt_eventInitDict
 * @constructor
 * @extends {Event}
 */
function RelatedEvent(type, opt_eventInitDict) {}

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-relatedevent-relatedtarget
 * @type {EventTarget|undefined}
 */
RelatedEvent.prototype.relatedTarget;


/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#the-dialog-element
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLDialogElement() {}

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-dialog-open
 * @type {boolean}
 */
HTMLDialogElement.prototype.open;

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-dialog-returnvalue
 * @type {string}
 */
HTMLDialogElement.prototype.returnValue;

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-dialog-show
 * @param {(MouseEvent|Element)=} opt_anchor
 * @return {undefined}
 */
HTMLDialogElement.prototype.show = function(opt_anchor) {};

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-dialog-showmodal
 * @param {(MouseEvent|Element)=} opt_anchor
 * @return {undefined}
 */
HTMLDialogElement.prototype.showModal = function(opt_anchor) {};

/**
 * @see http://www.w3.org/html/wg/drafts/html/master/interactive-elements.html#dom-dialog-close
 * @param {string=} opt_returnValue
 * @return {undefined}
 */
HTMLDialogElement.prototype.close = function(opt_returnValue) {};


/**
 * @see https://html.spec.whatwg.org/multipage/scripting.html#the-template-element
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLTemplateElement() {}

/**
 * @see https://html.spec.whatwg.org/multipage/scripting.html#the-template-element
 * @type {!DocumentFragment}
 */
HTMLTemplateElement.prototype.content;

/** @type {boolean} */
HTMLTemplateElement.prototype.shadowRootClonable;

/** @type {boolean} */
HTMLTemplateElement.prototype.shadowRootDelegatesFocus;

/** @type {string} */
HTMLTemplateElement.prototype.shadowRootMode;

/** @type {boolean} */
HTMLTemplateElement.prototype.shadowRootSerializable;

/**
 * @type {?Document}
 * @see w3c_dom2.js
 * @see http://www.w3.org/TR/html-imports/#interface-import
 */
HTMLLinkElement.prototype.import;

/**
 * @type {string}
 * @see https://html.spec.whatwg.org/#attr-link-as
 * @see https://w3c.github.io/preload/#as-attribute
 */
HTMLLinkElement.prototype.as;

/**
 * @see https://html.spec.whatwg.org/#attr-link-crossorigin
 * @type {string}
 */
HTMLLinkElement.prototype.crossOrigin;

/** @type {string} */
HTMLLinkElement.prototype.imageSizes;

/** @type {string} */
HTMLLinkElement.prototype.imageSrcset;

/**
 * @return {boolean}
 * @see https://www.w3.org/TR/html5/forms.html#dom-fieldset-elements
 */
HTMLFieldSetElement.prototype.checkValidity = function() {};

/**
 * @type {HTMLCollection}
 * @see https://www.w3.org/TR/html5/forms.html#dom-fieldset-elements
 */
HTMLFieldSetElement.prototype.elements;

/**
 * @type {string}
 * @see https://www.w3.org/TR/html5/forms.html#the-fieldset-element
 */
HTMLFieldSetElement.prototype.name;

/**
 * @param {string} message
 * @see https://www.w3.org/TR/html5/forms.html#dom-fieldset-elements
 * @return {undefined}
 */
HTMLFieldSetElement.prototype.setCustomValidity = function(message) {};

/**
 * @type {string}
 * @see https://www.w3.org/TR/html5/forms.html#dom-fieldset-type
 */
HTMLFieldSetElement.prototype.type;

/**
 * @type {string}
 * @see https://www.w3.org/TR/html5/forms.html#the-fieldset-element
 */
HTMLFieldSetElement.prototype.validationMessage;

/**
 * @type {ValidityState}
 * @see https://www.w3.org/TR/html5/forms.html#the-fieldset-element
 */
HTMLFieldSetElement.prototype.validity;

/**
 * @type {boolean}
 * @see https://www.w3.org/TR/html5/forms.html#the-fieldset-element
 */
HTMLFieldSetElement.prototype.willValidate;

/**
 * @constructor
 * @extends {NodeList<T>}
 * @template T
 * @see https://html.spec.whatwg.org/multipage/infrastructure.html#radionodelist
 */
function RadioNodeList() {}

/** @override */
RadioNodeList.prototype[Symbol.iterator] = function() {};

/**
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/infrastructure.html#radionodelist
 */
RadioNodeList.prototype.value;

/**
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLDataElement() {}

/** @type {string} */
HTMLDataElement.prototype.value;

/**
 * @see https://html.spec.whatwg.org/multipage/forms.html#the-datalist-element
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLDataListElement() {}


/** @type {HTMLCollection<!HTMLOptionElement>} */
HTMLDataListElement.prototype.options;

/**
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLTimeElement() {}

/** @type {string} */
HTMLTimeElement.prototype.dateTime;

/**
 * @return {boolean}
 * @see https://html.spec.whatwg.org/multipage/iframe-embed-object.html#the-object-element
 */
HTMLObjectElement.prototype.checkValidity;

/**
 * @param {string} message
 * @see https://html.spec.whatwg.org/multipage/iframe-embed-object.html#the-object-element
 * @return {undefined}
 */
HTMLObjectElement.prototype.setCustomValidity;

/**
 * @type {string}
 * @see https://html.spec.whatwg.org/multipage/iframe-embed-object.html#the-object-element
 */
HTMLObjectElement.prototype.validationMessage;

/**
 * @type {!ValidityState}
 * @see https://html.spec.whatwg.org/multipage/iframe-embed-object.html#the-object-element
 */
HTMLObjectElement.prototype.validity;

/**
 * @type {boolean}
 * @see https://html.spec.whatwg.org/multipage/iframe-embed-object.html#the-object-element
 */
HTMLObjectElement.prototype.willValidate;


/**
 * @see https://html.spec.whatwg.org/multipage/forms.html#the-output-element
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLOutputElement() {}

/**
 * @const {!DOMTokenList}
 */
HTMLOutputElement.prototype.htmlFor;

/**
 * @type {HTMLFormElement}
 */
HTMLOutputElement.prototype.form;

/**
 * @type {string}
 */
HTMLOutputElement.prototype.name;

/**
 * @const {string}
 */
HTMLOutputElement.prototype.type;

/**
 * @type {string}
 */
HTMLOutputElement.prototype.defaultValue;

/**
 * @type {string}
 */
HTMLOutputElement.prototype.value;

/**
 * @const {?NodeList<!HTMLLabelElement>}
 */
HTMLOutputElement.prototype.labels;

/** @type {string} */
HTMLOutputElement.prototype.validationMessage;

/**
 * @const {ValidityState}
 */
HTMLOutputElement.prototype.validity;

/** @type {boolean} */
HTMLOutputElement.prototype.willValidate;

/** @return {boolean} */
HTMLOutputElement.prototype.checkValidity = function() {};

/** @return {boolean} */
HTMLOutputElement.prototype.reportValidity = function() {};

/** @param {string} message */
HTMLOutputElement.prototype.setCustomValidity = function(message) {};



/**
 * @see https://html.spec.whatwg.org/multipage/forms.html#the-progress-element
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLProgressElement() {}


/** @type {number} */
HTMLProgressElement.prototype.value;


/** @type {number} */
HTMLProgressElement.prototype.max;


/** @type {number} */
HTMLProgressElement.prototype.position;


/** @type {?NodeList<!Node>} */
HTMLProgressElement.prototype.labels;



/**
 * @see https://html.spec.whatwg.org/multipage/embedded-content.html#the-track-element
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLTrackElement() {}

/** @const {number} */
HTMLTrackElement.prototype.NONE;

/** @const {number} */
HTMLTrackElement.prototype.LOADING;

/** @const {number} */
HTMLTrackElement.prototype.LOADED;

/** @const {number} */
HTMLTrackElement.prototype.ERROR;

/** @type {string} */
HTMLTrackElement.prototype.kind;


/** @type {string} @implicitCast */
HTMLTrackElement.prototype.src;


/** @type {string} */
HTMLTrackElement.prototype.srclang;


/** @type {string} */
HTMLTrackElement.prototype.label;


/** @type {boolean} */
HTMLTrackElement.prototype.default;


/** @const {number} */
HTMLTrackElement.prototype.readyState;


/** @const {!TextTrack} */
HTMLTrackElement.prototype.track;



/**
 * @see https://html.spec.whatwg.org/multipage/forms.html#the-meter-element
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLMeterElement() {}


/** @type {number} */
HTMLMeterElement.prototype.value;


/** @type {number} */
HTMLMeterElement.prototype.min;


/** @type {number} */
HTMLMeterElement.prototype.max;


/** @type {number} */
HTMLMeterElement.prototype.low;


/** @type {number} */
HTMLMeterElement.prototype.high;


/** @type {number} */
HTMLMeterElement.prototype.optimum;


/** @type {?NodeList<!Node>} */
HTMLMeterElement.prototype.labels;


/**
 * @interface
 * @see https://www.w3.org/TR/badging/
 */
function NavigatorBadge() {};

/**
 * @see https://www.w3.org/TR/badging/#setappbadge-method
 * @param {number=} contents
 * @return {Promise<undefined>}
 */
NavigatorBadge.prototype.setAppBadge = function(contents) {};

/**
 * @see https://www.w3.org/TR/badging/#clearappbadge-method
 * @return {Promise<undefined>}
 */
NavigatorBadge.prototype.clearAppBadge = function() {};

/**
 * @interface
 * @see https://storage.spec.whatwg.org/#api
 */
function NavigatorStorage() {};

/**
 * @type {!StorageManager}
 */
NavigatorStorage.prototype.storage;

/**
 * @constructor
 * @implements NavigatorBadge
 * @implements NavigatorStorage
 * @see https://www.w3.org/TR/html5/webappapis.html#navigator
 */
function Navigator() {}

/**
 * @type {string}
 * @see https://www.w3.org/TR/html5/webappapis.html#dom-navigator-appcodename
 */
Navigator.prototype.appCodeName;

/**
 * @type {string}
 * @see https://www.w3.org/TR/html5/webappapis.html#dom-navigator-appname
 */
Navigator.prototype.appName;

/**
 * @type {string}
 * @see https://www.w3.org/TR/html5/webappapis.html#dom-navigator-appversion
 */
Navigator.prototype.appVersion;

/**
 * @type {string}
 * @see https://www.w3.org/TR/html5/webappapis.html#dom-navigator-platform
 */
Navigator.prototype.platform;

/**
 * @type {string}
 * @see https://www.w3.org/TR/html5/webappapis.html#dom-navigator-product
 */
Navigator.prototype.product;

/**
 * @type {string}
 * @see https://www.w3.org/TR/html5/webappapis.html#dom-navigator-useragent
 */
Navigator.prototype.userAgent;

/**
 * @return {boolean}
 * @see https://www.w3.org/TR/html5/webappapis.html#dom-navigator-taintenabled
 */
Navigator.prototype.taintEnabled = function() {};

/**
 * @type {string}
 * @see https://www.w3.org/TR/html5/webappapis.html#dom-navigator-language
 */
Navigator.prototype.language;

/**
 * @type {!Array<string>|undefined}
 * @see https://html.spec.whatwg.org/multipage/system-state.html#dom-navigator-languages-dev
 */
Navigator.prototype.languages;

/**
 * @type {boolean}
 * @see https://www.w3.org/TR/html5/browsers.html#navigatoronline
 */
Navigator.prototype.onLine;

/**
 * @type {boolean}
 * @see https://www.w3.org/TR/html5/webappapis.html#dom-navigator-cookieenabled
 */
Navigator.prototype.cookieEnabled;

/**
 * @param {string} scheme
 * @param {!URL|string} url
 * @param {string} title
 * @return {undefined}
 */
Navigator.prototype.registerProtocolHandler = function(scheme, url, title) {};

/**
 * @param {string} mimeType
 * @param {string} url
 * @param {string} title
 * @return {undefined}
 */
Navigator.prototype.registerContentHandler = function(mimeType, url, title) {};

/**
 * @param {string} scheme
 * @param {!URL|string} url
 * @return {undefined}
 */
Navigator.prototype.unregisterProtocolHandler = function(scheme, url) {};

/**
 * @param {string} mimeType
 * @param {string} url
 * @return {undefined}
 */
Navigator.prototype.unregisterContentHandler = function(mimeType, url) {};

/**
 * @type {!MimeTypeArray}
 * @see https://www.w3.org/TR/html5/webappapis.html#dom-navigator-mimetypes
 */
Navigator.prototype.mimeTypes;

/**
 * @type {!PluginArray}
 * @see https://www.w3.org/TR/html5/webappapis.html#dom-navigator-plugins
 */
Navigator.prototype.plugins;

/**
 * @return {boolean}
 * @see https://www.w3.org/TR/html5/webappapis.html#dom-navigator-javaenabled
 * @nosideeffects
 */
Navigator.prototype.javaEnabled = function() {};

/**
 * @type {number}
 * @see https://developers.google.com/web/updates/2017/12/device-memory
 * https://github.com/w3c/device-memory
 */
Navigator.prototype.deviceMemory;

/**
 * @type {!StorageManager}
 * @see https://storage.spec.whatwg.org
 */
Navigator.prototype.storage;

/**
 * @param {!ShareData=} data
 * @return {boolean}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/Navigator/canShare
 */
Navigator.prototype.canShare = function(data) {};

/**
 * @param {!ShareData=} data
 * @return {!Promise<undefined>}
 * @see https://wicg.github.io/web-share/#share-method
 */
Navigator.prototype.share = function(data) {};

/**
 * @type {number}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/NavigatorConcurrentHardware/hardwareConcurrency
 */
Navigator.prototype.hardwareConcurrency;

/**
 * @type {UserActivation|undefined}
 * @see https://html.spec.whatwg.org/multipage/interaction.html#tracking-user-activation
 */
Navigator.prototype.userActivation;

/**
 * @type {boolean}
 * @see https://w3c.github.io/webdriver/#dfn-webdriver
 */
Navigator.prototype.webdriver;

/**
 * @see https://www.w3.org/TR/badging/#setappbadge-method
 * @param {number=} contents
 * @return {Promise<undefined>}
 * @override
 */
Navigator.prototype.setAppBadge = function(contents) {};

/**
 * @see https://www.w3.org/TR/badging/#clearappbadge-method
 * @return {Promise<undefined>}
 * @override
 */
Navigator.prototype.clearAppBadge = function() {};

/**
 * @type {boolean}
 * @see https://developer.mozilla.org/docs/Web/API/Navigator/pdfViewerEnabled
 */
Navigator.prototype.pdfViewerEnabled;

/**
 * @type {?string}
 */
Navigator.prototype.doNotTrack;

/**
 * @constructor
 * @implements NavigatorBadge
 * @implements NavigatorStorage
 * @see https://html.spec.whatwg.org/multipage/workers.html#the-workernavigator-object
 */
function WorkerNavigator() {}

/**
 * @type {number}
 * @see https://developers.google.com/web/updates/2017/12/device-memory
 * https://github.com/w3c/device-memory
 */
WorkerNavigator.prototype.deviceMemory;

/**
 * @type {number}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/NavigatorConcurrentHardware/hardwareConcurrency
 */
WorkerNavigator.prototype.hardwareConcurrency;

/**
 * @type {!StorageManager}
 * @see https://storage.spec.whatwg.org
 */
WorkerNavigator.prototype.storage;

/**
 * @see https://www.w3.org/TR/badging/#setappbadge-method
 * @param {number=} contents
 * @return {Promise<undefined>}
 * @override
 */
WorkerNavigator.prototype.setAppBadge = function(contents) {};

/**
 * @see https://www.w3.org/TR/badging/#clearappbadge-method
 * @return {Promise<undefined>}
 * @override
 */
WorkerNavigator.prototype.clearAppBadge = function() {};

/**
 * @record
 * @see https://wicg.github.io/web-share/#sharedata-dictionary
 */
function ShareData() {}

/** @type {string|undefined} */
ShareData.prototype.title;

/** @type {string|undefined} */
ShareData.prototype.text;

/** @type {string|undefined} */
ShareData.prototype.url;

/**
 * @constructor
 * @implements {IObject<(string|number),!Plugin>}
 * @implements {IArrayLike<!Plugin>}
 * @see https://www.w3.org/TR/html5/webappapis.html#pluginarray
 */
function PluginArray() {}

/** @type {number} */
PluginArray.prototype.length;

/**
 * @param {number} index
 * @return {Plugin}
 */
PluginArray.prototype.item = function(index) {};

/**
 * @param {string} name
 * @return {Plugin}
 */
PluginArray.prototype.namedItem = function(name) {};

/**
 * @param {boolean=} reloadDocuments
 * @return {undefined}
 */
PluginArray.prototype.refresh = function(reloadDocuments) {};

/**
 * @constructor
 * @implements {IObject<(string|number),!MimeType>}
 * @implements {IArrayLike<!MimeType>}
 * @see https://www.w3.org/TR/html5/webappapis.html#mimetypearray
 */
function MimeTypeArray() {}

/**
 * @param {number} index
 * @return {MimeType}
 */
MimeTypeArray.prototype.item = function(index) {};

/**
 * @type {number}
 * @see https://developer.mozilla.org/en/DOM/window.navigator.mimeTypes
 */
MimeTypeArray.prototype.length;

/**
 * @param {string} name
 * @return {MimeType}
 */
MimeTypeArray.prototype.namedItem = function(name) {};

/**
 * @constructor
 * @see https://www.w3.org/TR/html5/webappapis.html#mimetype
 */
function MimeType() {}

/** @type {string} */
MimeType.prototype.description;

/** @type {Plugin} */
MimeType.prototype.enabledPlugin;

/** @type {string} */
MimeType.prototype.suffixes;

/** @type {string} */
MimeType.prototype.type;

/**
 * @constructor
 * @see https://www.w3.org/TR/html5/webappapis.html#dom-plugin
 */
function Plugin() {}

/** @type {string} */
Plugin.prototype.description;

/** @type {string} */
Plugin.prototype.filename;

/** @type {number} */
Plugin.prototype.length;

/** @type {string} */
Plugin.prototype.name;

/**
 * @see https://html.spec.whatwg.org/multipage/custom-elements.html#customelementregistry
 * @constructor
 */
function CustomElementRegistry() {}

/**
 * @param {string} tagName
 * @param {function(new:HTMLElement)} klass
 * @param {{extends: string}=} options
 * @return {undefined}
 */
CustomElementRegistry.prototype.define = function(tagName, klass, options) {};

/**
 * @param {string} tagName
 * @return {function(new:HTMLElement)|undefined}
 */
CustomElementRegistry.prototype.get = function(tagName) {};

/**
 * @param {function(new:HTMLElement)} constructor
 * @return {string|null}
 */
CustomElementRegistry.prototype.getName = function(constructor) {};

/**
 * @param {string} tagName
 * @return {!Promise<undefined>}
 */
CustomElementRegistry.prototype.whenDefined = function(tagName) {};

/**
 * @param {!Node} root
 * @return {undefined}
 */
CustomElementRegistry.prototype.upgrade = function(root) {};

/** @type {!CustomElementRegistry} */
var customElements;

/** @type {!Navigator} */
var clientInformation;

/**
 * @constructor
 * @extends {HTMLElement}
 */
function HTMLSlotElement() {}

/** @typedef {{flatten: boolean}} */
var AssignedNodesOptions;

/**
 * @param {!AssignedNodesOptions=} options
 * @return {!Array<!Node>}
 */
HTMLSlotElement.prototype.assignedNodes = function(options) {};

/**
 * @param {!AssignedNodesOptions=} options
 * @return {!Array<!HTMLElement>}
 */
HTMLSlotElement.prototype.assignedElements = function(options) {};

/** @type {boolean} */
Event.prototype.composed;

/**
 * @return {!Array<!EventTarget>}
 * @see https://developer.mozilla.org/en-US/docs/Web/API/Event/composedPath
 */
Event.prototype.composedPath = function() {};

/**
 * @constructor
 * @param {{
 *     firesTouchEvents: (string|undefined),
 *     pointerMovementScrolls: (string|undefined)
 *   }=} opt_options
 */
function InputDeviceCapabilities(opt_options) {}

/** @type {boolean} */
InputDeviceCapabilities.prototype.firesTouchEvents;

/** @type {boolean} */
InputDeviceCapabilities.prototype.pointerMovementScrolls;

/** @type {?InputDeviceCapabilities} */
UIEvent.prototype.sourceCapabilities;

/**
 * @see https://developer.mozilla.org/en-US/docs/Web/API/VisualViewport
 * @constructor
 * @implements {EventTarget}
 */
function VisualViewport() {}

/** @type {number} */
VisualViewport.prototype.offsetLeft;

/** @type {number} */
VisualViewport.prototype.offsetTop;

/** @type {number} */
VisualViewport.prototype.pageLeft;

/** @type {number} */
VisualViewport.prototype.pageTop;

/** @type {number} */
VisualViewport.prototype.width;

/** @type {number} */
VisualViewport.prototype.height;

/** @type {number} */
VisualViewport.prototype.scale;

/** @override */
VisualViewport.prototype.addEventListener = function(
    type, listener, opt_options) {};

/** @override */
VisualViewport.prototype.removeEventListener = function(
    type, listener, opt_options) {};

/** @override */
VisualViewport.prototype.dispatchEvent = function(evt) {};

/** @type {?function(!Event)} */
VisualViewport.prototype.onresize;

/** @type {?function(!Event)} */
VisualViewport.prototype.onscroll;

/**
 * @see https://storage.spec.whatwg.org/
 * @constructor
 */
function StorageManager() {}

/** @return {!Promise<boolean>} */
StorageManager.prototype.persisted = function() {};

/** @return {!Promise<boolean>} */
StorageManager.prototype.persist = function() {};

/** @return {!Promise<StorageEstimate>} */
StorageManager.prototype.estimate = function() {};

/**
 * @see https://storage.spec.whatwg.org/
 * @typedef {{
 *   usage: number,
 *   quota: number
 * }}
 */
var StorageEstimate;

/*
 * Focus Management APIs
 *
 * @see https://html.spec.whatwg.org/multipage/interaction.html#focus-management-apis
 */


/**
 * @type {?Element}
 * @see https://html.spec.whatwg.org/multipage/interaction.html#dom-document-activeelement
 */
Document.prototype.activeElement;

/**
 * @see https://html.spec.whatwg.org/multipage/interaction.html#dom-document-hasfocus
 * @return {boolean}
 */
Document.prototype.hasFocus = function() {};

/**
 * @param {{preventScroll: boolean}=} options
 * @return {undefined}
 * @see https://html.spec.whatwg.org/multipage/interaction.html#dom-focus
 */
Element.prototype.focus = function(options) {};

/**
 * @return {undefined}
 * @see https://html.spec.whatwg.org/multipage/interaction.html#dom-blur
 */
Element.prototype.blur = function() {};

/**
 * @see https://www.w3.org/TR/CSP3/#securitypolicyviolationevent
 *
 * @constructor
 * @extends {Event}
 *
 * @param {string} type
 * @param {SecurityPolicyViolationEventInit=}
 *     opt_securityPolicyViolationEventInitDict
 */
function SecurityPolicyViolationEvent(
    type, opt_securityPolicyViolationEventInitDict) {}

/** @const {string} */
SecurityPolicyViolationEvent.prototype.documentURI;

/** @const {string} */
SecurityPolicyViolationEvent.prototype.referrer;

/** @const {string} */
SecurityPolicyViolationEvent.prototype.blockedURI;

/** @const {string} */
SecurityPolicyViolationEvent.prototype.effectiveDirective;

/** @const {string} */
SecurityPolicyViolationEvent.prototype.violatedDirective;

/** @const {string} */
SecurityPolicyViolationEvent.prototype.originalPolicy;

/** @const {string} */
SecurityPolicyViolationEvent.prototype.sourceFile;

/** @const {string} */
SecurityPolicyViolationEvent.prototype.sample;

/**
 * @see https://www.w3.org/TR/CSP3/#enumdef-securitypolicyviolationeventdisposition
 * @const {string}
 */
SecurityPolicyViolationEvent.prototype.disposition;

/** @const {number} */
SecurityPolicyViolationEvent.prototype.statusCode;

/** @const {number} */
SecurityPolicyViolationEvent.prototype.lineNumber;

/** @const {number} */
SecurityPolicyViolationEvent.prototype.columnNumber;



/**
 * @record
 * @extends {EventInit}
 * @see https://www.w3.org/TR/CSP3/#dictdef-securitypolicyviolationeventinit
 */
function SecurityPolicyViolationEventInit() {}

/** @type {string} */
SecurityPolicyViolationEventInit.prototype.documentURI;

/** @type {undefined|string} */
SecurityPolicyViolationEventInit.prototype.referrer;

/** @type {undefined|string} */
SecurityPolicyViolationEventInit.prototype.blockedURI;

/** @type {string} */
SecurityPolicyViolationEventInit.prototype.disposition;

/** @type {string} */
SecurityPolicyViolationEventInit.prototype.effectiveDirective;

/** @type {string} */
SecurityPolicyViolationEventInit.prototype.violatedDirective;

/** @type {string} */
SecurityPolicyViolationEventInit.prototype.originalPolicy;

/** @type {undefined|string} */
SecurityPolicyViolationEventInit.prototype.sourceFile;

/** @type {undefined|string} */
SecurityPolicyViolationEventInit.prototype.sample;

/** @type {number} */
SecurityPolicyViolationEventInit.prototype.statusCode;

/** @type {undefined|number} */
SecurityPolicyViolationEventInit.prototype.lineNumber;

/** @type {undefined|number} */
SecurityPolicyViolationEventInit.prototype.columnNumber;


/**
 * @record
 * @extends {EventInit}
 * @see https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#the-submitevent-interface
 */
function SubmitEventInit() {}

/** @type {undefined|?HTMLElement} */
SubmitEventInit.prototype.submitter;

/**
 * @constructor
 * @extends {Event}
 * @param {string} type
 * @param {SubmitEventInit=} opt_eventInitDict
 * @see https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#the-submitevent-interface
 */
function SubmitEvent(type, opt_eventInitDict) {}

/** @type {undefined|!HTMLElement} */
SubmitEvent.prototype.submitter;


/**
 * @see https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#formdataevent
 *
 * @constructor
 * @extends {Event}
 *
 * @param {string} type
 * @param {FormDataEventInit=} eventInitDict
 */
function FormDataEvent(type, eventInitDict) {}

/** @const {!FormData} */
FormDataEvent.prototype.formData;

/**
 * @see https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#formdataeventinit
 *
 * @record
 * @extends {EventInit}
 */
function FormDataEventInit() {}

/** @type {!FormData} */
FormDataEventInit.prototype.formData;

/**
 * @see https://html.spec.whatwg.org/multipage/indices.html#event-formdata
 * @type {?function(FormDataEvent)}
 */
HTMLFormElement.prototype.onformdata;

/**
 * @const {boolean}
 * Whether the document has opted in to cross-origin isolation.
 * @see https://html.spec.whatwg.org/multipage/webappapis.html#dom-crossoriginisolated
 */
Window.prototype.crossOriginIsolated;

/**
 * @see https://html.spec.whatwg.org/multipage/interaction.html#tracking-user-activation
 *
 * @record
 */
function UserActivation() {}

/** @type {boolean} */
UserActivation.prototype.isActive;

/** @type {boolean} */
UserActivation.prototype.hasBeenActive;

/**
 * @see https://html.spec.whatwg.org/multipage/custom-elements.html#the-elementinternals-interface
 * @constructor
 */
function ElementInternals() {}

/** @type {!ShadowRoot|null} */
ElementInternals.prototype.shadowRoot;

/**
 * @param {!File|string|!FormData|null} value
 * @param {!File|string|!FormData|null} state
 */
ElementInternals.prototype.setFormValue = function(value, state) {};

/** @type {!HTMLFormElement|null} */
ElementInternals.prototype.form;

/**
 * @param {!ValidityStateFlags|undefined} flags
 * @param {string|undefined} message
 * @param {!HTMLElement|undefined} anchor
 */
ElementInternals.prototype.setValidity = function(flags, message, anchor) {};

/** @type {boolean} */
ElementInternals.prototype.willValidate;

/** @type {!ValidityState} */
ElementInternals.prototype.validity;

/** @type {string} */
ElementInternals.prototype.validationMessage;

/**
 * @return {boolean}
 */
ElementInternals.prototype.checkValidity =
    function() {}

    /**
     * @return {boolean}
     */
    ElementInternals.prototype.reportValidity =
        function() {}

        /** @type {!NodeList<!HTMLLabelElement>} */
        ElementInternals.prototype.labels;

/** @type {!CustomStateSet} */
ElementInternals.prototype.states;

/**
 * @see https://html.spec.whatwg.org/multipage/custom-elements.html#the-elementinternals-interface
 * @record
 */
function ValidityStateFlags() {}

/** @type {boolean|undefined} */
ValidityStateFlags.prototype.valueMissing;

/** @type {boolean|undefined} */
ValidityStateFlags.prototype.typeMismatch;

/** @type {boolean|undefined} */
ValidityStateFlags.prototype.patternMismatch;

/** @type {boolean|undefined} */
ValidityStateFlags.prototype.tooLong;

/** @type {boolean|undefined} */
ValidityStateFlags.prototype.tooShort;

/** @type {boolean|undefined} */
ValidityStateFlags.prototype.rangeUnderflow;

/** @type {boolean|undefined} */
ValidityStateFlags.prototype.rangeOverflow;

/** @type {boolean|undefined} */
ValidityStateFlags.prototype.stepMismatch;

/** @type {boolean|undefined} */
ValidityStateFlags.prototype.badInput;

/** @type {boolean|undefined} */
ValidityStateFlags.prototype.customError;

/**
 * @see https://html.spec.whatwg.org/multipage/custom-elements.html#the-elementinternals-interface
 * @constructor
 */
function CustomStateSet() {}

/**
 * @param {string} value
 * @return {void}
 */
CustomStateSet.prototype.add = function(value) {};

/**
 * @return {void}
 */
CustomStateSet.prototype.clear = function() {};

/**
 * @param {string} value
 * @return {boolean}
 */
CustomStateSet.prototype.delete = function(value) {};

/**
 * @return {!IteratorIterable<!Array<string>>} Where each array has two entries:
 *     [value, value]
 * @nosideeffects
 */
CustomStateSet.prototype.entries = function() {};

/**
 * @param {function(this: THIS, string, string, CustomStateSet)} callback
 * @param {THIS=} opt_thisArg
 * @this {THIS}
 * @template THIS
 */
CustomStateSet.prototype.forEach = function(callback, opt_thisArg) {};

/**
 * @param {string} value
 * @return {boolean}
 * @nosideeffects
 */
CustomStateSet.prototype.has = function(value) {};

/**
 * @type {number} (readonly)
 */
CustomStateSet.prototype.size;

/**
 * @return {!IteratorIterable<string>}
 * @nosideeffects
 */
CustomStateSet.prototype.keys = function() {};

/**
 * @return {!IteratorIterable<string>}
 * @nosideeffects
 */
CustomStateSet.prototype.values = function() {};

/**
 * @return {!IteratorIterable<string>}
 */
CustomStateSet.prototype[Symbol.iterator] = function() {};

/**
 * @see https://drafts.csswg.org/css-view-transitions/#dom-document-startviewtransition
 * @param {function(): (undefined|!Promise<undefined>)} updateCallback
 * @return {!ViewTransition}
 */
Document.prototype.startViewTransition = function(updateCallback) {};

/**
 * @see https://drafts.csswg.org/css-view-transitions/#viewtransition
 * @record
 * @struct
 */
function ViewTransition() {}

/** @const {!Promise<undefined>} */
ViewTransition.prototype.finished;

/** @const {!Promise<undefined>} */
ViewTransition.prototype.ready;

/** @const {!Promise<undefined>} */
ViewTransition.prototype.updateCallbackDone;

/** @return {undefined} */
ViewTransition.prototype.skipTransition = function() {};

/** @const {!ViewTransitionTypeSet} */
ViewTransition.prototype.types;


/**
 * @constructor
 */
function ViewTransitionTypeSet() {}

/**
 * @param {function(this: THIS, string, string, CustomStateSet)} callback
 * @param {THIS=} opt_thisArg
 * @this {THIS}
 * @template THIS
 */
ViewTransitionTypeSet.prototype.forEach = function(callback, opt_thisArg) {};

/**
 * @record
 * @extends {EventInit}
 * @see https://html.spec.whatwg.org/multipage/interaction.html#toggleeventinit
 */
function ToggleEventInit() {}

/** @type {undefined|string} */
ToggleEventInit.prototype.newState;

/** @type {undefined|string} */
ToggleEventInit.prototype.oldState;

/**
 * @param {string} type
 * @param {ToggleEventInit=} opt_eventInitDict
 * @see https://html.spec.whatwg.org/multipage/interaction.html#toggleevent
 * @constructor
 * @extends {Event}
 */
function ToggleEvent(type, opt_eventInitDict) {}

/** @const {string} */
ToggleEvent.prototype.newState;

/** @const {string} */
ToggleEvent.prototype.oldState;
