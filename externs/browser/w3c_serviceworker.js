/*
 * Copyright 2014 The Closure Compiler Authors
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
 * @fileoverview Externs for service worker.
 *
 * @see http://www.w3.org/TR/service-workers/
 * @externs
 */

/**
 * @see http://www.w3.org/TR/service-workers/#service-worker-interface
 * @constructor
 * @extends {Worker}
 */
function ServiceWorker() {}

/** @type {string} */
ServiceWorker.prototype.scriptURL;

/** @type {ServiceWorkerState} */
ServiceWorker.prototype.state;

/** @type {?function(!Event)} */
ServiceWorker.prototype.onstatechange;

/**
 *  Set of possible string values: 'installing', 'installed', 'activating',
 * 'activated', 'redundant'.
 *  @typedef {string}
 */
var ServiceWorkerState;

/**
 * @see https://w3c.github.io/ServiceWorker/#navigationpreloadmanager
 * @constructor
 */
function NavigationPreloadManager() {}

/** @return {!Promise<void>} */
NavigationPreloadManager.prototype.enable = function() {};

/** @return {!Promise<void>} */
NavigationPreloadManager.prototype.disable = function() {};

/**
 * @param {string=} value
 * @return {!Promise<void>}
 */
NavigationPreloadManager.prototype.setHeaderValue = function(value) {};

/** @return {!Promise<NavigationPreloadState>} */
NavigationPreloadManager.prototype.getState = function() {};

/**
 *  @typedef {{
 *   enabled: (boolean|undefined),
 *   headerValue: (string|undefined)
 * }}
 */
var NavigationPreloadState;

/** @record */
function PushSubscriptionOptions() {}

/** @type {ArrayBuffer|undefined} */
PushSubscriptionOptions.prototype.applicationServerKey;

/** @type {boolean|undefined} */
PushSubscriptionOptions.prototype.userVisibleOnly;

/** @record */
function PushSubscriptionOptionsInit() {}

/** @type {BufferSource|string|undefined} */
PushSubscriptionOptionsInit.prototype.applicationServerKey;

/** @type {boolean|undefined} */
PushSubscriptionOptionsInit.prototype.userVisibleOnly;


/**
 * @see https://w3c.github.io/push-api/
 * @constructor
 */
function PushSubscription() {}

/** @type {string} */
PushSubscription.prototype.endpoint;

/** @type {number|null} */
PushSubscription.prototype.expirationTime;

/**
 * Please note there is an intent to deprecate this field in Chrome 43 or 44.
 * See https://www.chromestatus.com/feature/5283829761703936.
 * @type {string}
 */
PushSubscription.prototype.subscriptionId;

/** @type {!PushSubscriptionOptions} */
PushSubscription.prototype.options;

/** @return {!Promise<boolean>} */
PushSubscription.prototype.unsubscribe = function() {};

/**
 * @param {string} name
 * @return {!ArrayBuffer|null}
 */
PushSubscription.prototype.getKey = function(name) {};


/**
 * @see https://w3c.github.io/push-api/#idl-def-PushManager
 * @constructor
 */
function PushManager() {}

/**
 * @const {!Array<string>}
 */
PushManager.supportedContentEncodings;

/**
 * @param {PushSubscriptionOptionsInit=} opt_options
 * @return {!Promise<!PushSubscription>}
 */
PushManager.prototype.subscribe = function(opt_options) {};

/** @return {!Promise<PushSubscription>} */
PushManager.prototype.getSubscription = function() {};

/**
 * @param {PushSubscriptionOptionsInit=} options
 * @return {!Promise<string>}
 */
PushManager.prototype.permissionState = function(options) {};

/**
 * @see https://wicg.github.io/BackgroundSync/spec/#sync-manager-interface
 * @constructor
 */
function SyncManager() {}

/**
 * @param {string} tag
 * @return {!Promise<void>}
 */
SyncManager.prototype.register = function(tag) {}

/**
 * @return {!Promise<Array<string>>}
 */
SyncManager.prototype.getTags = function() {}

/**
 * @see https://wicg.github.io/BackgroundSync/spec/#sync-event
 * @constructor
 * @extends{ExtendableEvent}
 */
function SyncEvent() {}

/** @type {string} */
SyncEvent.prototype.tag;

/** @type {boolean} */
SyncEvent.prototype.lastChance;

/**
 * @see http://www.w3.org/TR/push-api/#idl-def-PushMessageData
 * @constructor
 */
function PushMessageData() {}

/** @return {!ArrayBuffer} */
PushMessageData.prototype.arrayBuffer = function() {};

/** @return {!Blob} */
PushMessageData.prototype.blob = function() {};

/** @return {*} */
PushMessageData.prototype.json = function() {};

/** @return {string} */
PushMessageData.prototype.text = function() {};


/**
 * @see http://www.w3.org/TR/push-api/#idl-def-PushEvent
 * @constructor
 * @param {string} type
 * @param {!ExtendableEventInit=} opt_eventInitDict
 * @extends {ExtendableEvent}
 */
function PushEvent(type, opt_eventInitDict) {}

/** @type {?PushMessageData} */
PushEvent.prototype.data;


/**
 * @see http://www.w3.org/TR/service-workers/#service-worker-registration-interface
 * @interface
 * @extends {EventTarget}
 */
function ServiceWorkerRegistration() {}

/** @type {ServiceWorker} */
ServiceWorkerRegistration.prototype.installing;

/** @type {ServiceWorker} */
ServiceWorkerRegistration.prototype.waiting;

/** @type {ServiceWorker} */
ServiceWorkerRegistration.prototype.active;

/** @type {NavigationPreloadManager} */
ServiceWorkerRegistration.prototype.navigationPreload;

/** @type {string} */
ServiceWorkerRegistration.prototype.scope;

/** @return {!Promise<boolean>} */
ServiceWorkerRegistration.prototype.unregister = function() {};

/** @type {?function(!Event)} */
ServiceWorkerRegistration.prototype.onupdatefound;

/** @return {!Promise<void>} */
ServiceWorkerRegistration.prototype.update = function() {};

/**
 * @see https://w3c.github.io/push-api/
 * @type {!PushManager}
 */
ServiceWorkerRegistration.prototype.pushManager;

/**
 * @see https://notifications.spec.whatwg.org/#service-worker-api
 * @param {string} title
 * @param {NotificationOptions=} opt_options
 * @return {!Promise<void>}
 */
ServiceWorkerRegistration.prototype.showNotification =
    function(title, opt_options) {};

/**
 * @see https://notifications.spec.whatwg.org/#service-worker-api
 * @param {!GetNotificationOptions=} opt_filter
 * @return {!Promise<?Array<?Notification>>}
 */
ServiceWorkerRegistration.prototype.getNotifications = function(opt_filter) {};

/**
 * @see https://wicg.github.io/BackgroundSync/spec/#service-worker-registration-extensions
 * @type {!SyncManager}
 */
ServiceWorkerRegistration.prototype.sync;

/**
 * @see http://www.w3.org/TR/service-workers/#service-worker-container-interface
 * @interface
 * @extends {EventTarget}
 */
function ServiceWorkerContainer() {}

/** @type {?ServiceWorker} */
ServiceWorkerContainer.prototype.controller;

/** @type {!Promise<!ServiceWorkerRegistration>} */
ServiceWorkerContainer.prototype.ready;

/**
 * @param {!TrustedScriptURL|!URL|string} scriptURL
 * @param {RegistrationOptions=} opt_options
 * @return {!Promise<!ServiceWorkerRegistration>}
 */
ServiceWorkerContainer.prototype.register = function(scriptURL, opt_options) {};

/**
 * @param {!URL|string=} documentURL
 * @return {!Promise<!ServiceWorkerRegistration|undefined>}
 */
ServiceWorkerContainer.prototype.getRegistration = function(documentURL) {};

/**
 * @return {!Promise<Array<!ServiceWorkerRegistration>>}
 */
ServiceWorkerContainer.prototype.getRegistrations = function() {};

/** @type {?function(!Event): void} */
ServiceWorkerContainer.prototype.oncontrollerchange;

/** @type {?function(!ExtendableMessageEvent): void} */
ServiceWorkerContainer.prototype.onmessage;

/** @type {?function(!MessageEvent<*>)} */
ServiceWorkerContainer.prototype.onmessageerror;

/** @type {?function(!ErrorEvent): void} */
ServiceWorkerContainer.prototype.onerror;

/** @return {undefined} */
ServiceWorkerContainer.prototype.startMessages = function() {};

/**
 * @typedef {{scope: (string|undefined), useCache: (boolean|undefined), updateViaCache: (string|undefined)}}
 */
var RegistrationOptions;

/** @type {!ServiceWorkerContainer} */
Navigator.prototype.serviceWorker;

/**
 * @see http://www.w3.org/TR/service-workers/#service-worker-global-scope-interface
 * @interface
 * @extends {WorkerGlobalScope}
 */
function ServiceWorkerGlobalScope() {}

/** @type {!Cache} */
ServiceWorkerGlobalScope.prototype.scriptCache;

/** @type {!CacheStorage} */
ServiceWorkerGlobalScope.prototype.caches;

/** @type {!ServiceWorkerClients} */
ServiceWorkerGlobalScope.prototype.clients;

/** @type {string} */
ServiceWorkerGlobalScope.prototype.scope;

/** @type {!ServiceWorkerRegistration} */
ServiceWorkerGlobalScope.prototype.registration;

/** @return {!Promise<void>} */
ServiceWorkerGlobalScope.prototype.skipWaiting = function() {};

/** @type {!Console} */
ServiceWorkerGlobalScope.prototype.console;

/** @type {?function(!InstallEvent)} */
ServiceWorkerGlobalScope.prototype.oninstall;

/** @type {?function(!ExtendableEvent)} */
ServiceWorkerGlobalScope.prototype.onactivate;

/** @type {?function(!FetchEvent)} */
ServiceWorkerGlobalScope.prototype.onfetch;

/**
 * TODO(mtragut): This handler should get a custom event in the future.
 * @type {?function(!Event)}
 */
ServiceWorkerGlobalScope.prototype.onbeforeevicted;

/**
 * TODO(mtragut): This handler should get a custom event in the future.
 * @type {?function(!Event)}
 */
ServiceWorkerGlobalScope.prototype.onevicted;

/** @type {?function(!MessageEvent)} */
ServiceWorkerGlobalScope.prototype.onmessage;

/**
 * While not strictly correct, this should be effectively correct. Notification
 * is the Notification constructor but calling it from the Service Worker throws
 * (https://notifications.spec.whatwg.org/#constructors) so its only use is as
 * an object holding some static properties (note that requestPermission is only
 * exposed to window context - https://notifications.spec.whatwg.org/#api).
 *
 * @type {{
 *   permission: string,
 *   maxActions: number,
 * }}
 */
ServiceWorkerGlobalScope.prototype.Notification;

/**
 * @see http://www.w3.org/TR/service-workers/#service-worker-client-interface
 * @constructor
 */
function ServiceWorkerClient() {}

/** @type {!Promise<void>} */
ServiceWorkerClient.prototype.ready;

/** @type {boolean} */
ServiceWorkerClient.prototype.hidden;

/** @type {boolean} */
ServiceWorkerClient.prototype.focused;

/** @type {VisibilityState} */
ServiceWorkerClient.prototype.visibilityState;

/** @type {string} */
ServiceWorkerClient.prototype.url;

/** @type {string} */
ServiceWorkerClient.prototype.id;

/**
 * // TODO(mtragut): Possibly replace the type with enum ContextFrameType once
 * the enum is defined.
 * @type {string}
 */
ServiceWorkerClient.prototype.frameType;

/**
 * @param {*} message
 * @param {(!Array<!Transferable>|undefined)=} opt_transfer
 * @return {undefined}
 */
ServiceWorkerClient.prototype.postMessage = function(message, opt_transfer) {};

/** @return {!Promise} */
ServiceWorkerClient.prototype.focus = function() {};

/**
 * @param {!URL|string} url
 * @return {!Promise<!ServiceWorkerClient>}
 */
ServiceWorkerClient.prototype.navigate = function(url) {};

/**
 * @see http://www.w3.org/TR/service-workers/#service-worker-clients-interface
 * @interface
 */
function ServiceWorkerClients() {}

/**
 * Deprecated in Chrome M43+, use matchAll instead. Reference:
 * https://github.com/slightlyoff/ServiceWorker/issues/610.
 * TODO(joeltine): Remove when getAll is fully deprecated.
 * @param {ServiceWorkerClientQueryOptions=} opt_options
 * @return {!Promise<!Array<!ServiceWorkerClient>>}
 */
ServiceWorkerClients.prototype.getAll = function(opt_options) {};

/**
 * @param {ServiceWorkerClientQueryOptions=} opt_options
 * @return {!Promise<!Array<!ServiceWorkerClient>>}
 */
ServiceWorkerClients.prototype.matchAll = function(opt_options) {};

/**
 * @return {!Promise<void>}
 */
ServiceWorkerClients.prototype.claim = function() {};

/**
 * @param {!URL|string} url
 * @return {!Promise<!ServiceWorkerClient>}
 */
ServiceWorkerClients.prototype.openWindow = function(url) {};

/**
 * @param {string} id
 * @return {!Promise<!ServiceWorkerClient|undefined>}
 */
ServiceWorkerClients.prototype.get = function(id) {};

/** @typedef {{includeUncontrolled: (boolean|undefined)}} */
var ServiceWorkerClientQueryOptions;

/**
 * @see http://www.w3.org/TR/service-workers/#cache-interface
 * @interface
 */
function Cache() {}

/**
 * @param {!RequestInfo} request
 * @param {CacheQueryOptions=} opt_options
 * @return {!Promise<!Response|undefined>}
 */
Cache.prototype.match = function(request, opt_options) {};

/**
 * @param {RequestInfo=} opt_request
 * @param {CacheQueryOptions=} opt_options
 * @return {!Promise<!Array<!Response>>}
 */
Cache.prototype.matchAll = function(opt_request, opt_options) {};

/**
 * @param {!RequestInfo} request
 * @return {!Promise<void>}
 */
Cache.prototype.add = function(request) {};

/**
 * @param {!Array<!RequestInfo>} requests
 * @return {!Promise<void>}
 */
Cache.prototype.addAll = function(requests) {};

/**
 * @param {!RequestInfo} request
 * @param {!Response} response
 * @return {!Promise<void>}
 */
Cache.prototype.put = function(request, response) {};

/**
 * @param {!RequestInfo} request
 * @param {CacheQueryOptions=} opt_options
 * @return {!Promise<boolean>}
 */
Cache.prototype.delete = function(request, opt_options) {};

/**
 * @param {RequestInfo=} opt_request
 * @param {CacheQueryOptions=} opt_options
 * @return {!Promise<!Array<!Request>>}
 */
Cache.prototype.keys = function(opt_request, opt_options) {};

/**
 * @typedef {{
 *   ignoreSearch: (boolean|undefined),
 *   ignoreMethod: (boolean|undefined),
 *   ignoreVary: (boolean|undefined),
 *   prefixMatch: (boolean|undefined),
 *   cacheName: (string|undefined)
 * }}
 */
var CacheQueryOptions;

/**
 * @see http://www.w3.org/TR/service-workers/#cache-storage-interface
 * @interface
 */
function CacheStorage() {}

/**
 * Window instances have a property called caches which implements CacheStorage
 * @see https://www.w3.org/TR/service-workers/#cache-objects
 * @type {!CacheStorage}
 */
Window.prototype.caches;

/**
 * @param {!RequestInfo} request
 * @param {CacheQueryOptions=} opt_options
 * @return {!Promise<!Response|undefined>}
 */
CacheStorage.prototype.match = function(request, opt_options) {};

/**
 * @param {string} cacheName
 * @return {!Promise<boolean>}
 */
CacheStorage.prototype.has = function(cacheName) {};

/**
 * @param {string} cacheName
 * @return {!Promise<!Cache>}
 */
CacheStorage.prototype.open = function(cacheName) {};

/**
 * @param {string} cacheName
 * @return {!Promise<boolean>}
 */
CacheStorage.prototype.delete = function(cacheName) {};

/** @return {!Promise<!Array<string>>} */
CacheStorage.prototype.keys = function() {};

/**
 * @see http://www.w3.org/TR/service-workers/#extendable-event-interface
 * @constructor
 * @param {string} type
 * @param {ExtendableEventInit=} opt_eventInitDict
 * @extends {Event}
 */
function ExtendableEvent(type, opt_eventInitDict) {}

/**
 * @param {IThenable} f
 * @return {undefined}
 */
ExtendableEvent.prototype.waitUntil = function(f) {};

/**
 * @typedef {{
 *   bubbles: (boolean|undefined),
 *   cancelable: (boolean|undefined)
 * }}
 */
var ExtendableEventInit;

/**
 * @see http://www.w3.org/TR/service-workers/#install-event-interface
 * @constructor
 * @param {string} type
 * @param {InstallEventInit=} opt_eventInitDict
 * @extends {ExtendableEvent}
 */
function InstallEvent(type, opt_eventInitDict) {}

/** @type {ServiceWorker} */
ExtendableEvent.prototype.activeWorker;

/**
 * @typedef {{
 *   bubbles: (boolean|undefined),
 *   cancelable: (boolean|undefined),
 *   activeWorker: (!ServiceWorker|undefined)
 * }}
 */
var InstallEventInit;

/**
 * @see http://www.w3.org/TR/service-workers/#fetch-event-interface
 * @constructor
 * @param {string} type
 * @param {FetchEventInit=} opt_eventInitDict
 * @extends {ExtendableEvent}
 */
function FetchEvent(type, opt_eventInitDict) {}

/** @type {!Request} */
FetchEvent.prototype.request;

/**
 * @type {!Promise<Response>}
 */
FetchEvent.prototype.preloadResponse;

/**
 * @type {!ServiceWorkerClient}
 * @deprecated
 */
FetchEvent.prototype.client;

/** @type {?string} */
FetchEvent.prototype.clientId;

/** @type {boolean} */
FetchEvent.prototype.isReload;

/** @type {?string} */
FetchEvent.prototype.resultingClientId;

/**
 * @param {(Response|IThenable<Response>)} r
 * @return {undefined}
 */
FetchEvent.prototype.respondWith = function(r) {};

/**
 * @param {string} url
 * @return {!Promise<!Response>}
 */
FetchEvent.prototype.forwardTo = function(url) {};

/**
 * @return {!Promise<!Response>}
 */
FetchEvent.prototype.default = function() {};

/**
 * @typedef {{
 *   bubbles: (boolean|undefined),
 *   cancelable: (boolean|undefined),
 *   request: (!Request|undefined),
 *   preloadResponse: (!Promise<Response>),
 *   client: (!ServiceWorkerClient|undefined),
 *   isReload: (boolean|undefined)
 * }}
 */
var FetchEventInit;


/**
 * @see https://www.w3.org/TR/service-workers/#extendablemessage-event-interface
 * @param {string} type
 * @param {!ExtendableMessageEventInit<T>=} opt_eventInitDict
 * @constructor
 * @extends {ExtendableEvent}
 * @template T
 */
function ExtendableMessageEvent(type, opt_eventInitDict) {};

/** @type {T} */
ExtendableMessageEvent.prototype.data;

/** @type {string} */
ExtendableMessageEvent.prototype.origin;

/** @type {string} */
ExtendableMessageEvent.prototype.lastEventId;

/** @type {?ServiceWorkerClient|?ServiceWorker|?MessagePort} */
ExtendableMessageEvent.prototype.source;

/** @type {?Array<!MessagePort>} */
ExtendableMessageEvent.prototype.ports;


/**
 * @see https://www.w3.org/TR/service-workers/#extendablemessage-event-init-dictionary
 * @record
 * @extends {ExtendableEventInit}
 * @template T
 */
function ExtendableMessageEventInit() {};

/** @type {T} */
ExtendableMessageEventInit.prototype.data;

/** @type {string|undefined} */
ExtendableMessageEventInit.prototype.origin;

/** @type {string|undefined} */
ExtendableMessageEventInit.prototype.lastEventId;

/** @type {!ServiceWorkerClient|!ServiceWorker|!MessagePort|undefined} */
ExtendableMessageEventInit.prototype.source;

/** @type {!Array<!MessagePort>|undefined} */
ExtendableMessageEventInit.prototype.ports;
