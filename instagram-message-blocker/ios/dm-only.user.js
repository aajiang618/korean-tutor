// ==UserScript==
// @name         DM Only for Instagram
// @namespace    https://github.com/aajiang618/korean-tutor
// @version      1.0.0
// @description  Hides the Instagram feed, Reels, Explore and stories in the browser. Messages, profiles, follow requests and people search keep working.
// @author       aajiang618
// @match        https://www.instagram.com/*
// @match        https://instagram.com/*
// @run-at       document-start
// @grant        none
// @downloadURL  https://raw.githubusercontent.com/aajiang618/korean-tutor/main/instagram-message-blocker/ios/dm-only.user.js
// @updateURL    https://raw.githubusercontent.com/aajiang618/korean-tutor/main/instagram-message-blocker/ios/dm-only.user.js
// ==/UserScript==

/*
 * Works in Safari on iPhone through the free "Userscripts" app, and in any desktop browser through
 * Tampermonkey / Violentmonkey. See ../README.md, section "iPhone".
 *
 * Instagram's web app changes the URL without reloading the page, so besides checking the address on
 * load we hook history.pushState / replaceState, listen for back/forward, and poll as a fallback.
 */
(function () {
  'use strict';

  // ---- settings ---------------------------------------------------------------------------------
  var INBOX = '/direct/inbox/';
  // Single posts (/p/...) use the feed layout. Set to false if you want to open posts from profiles.
  var BLOCK_SINGLE_POSTS = true;
  // Hide the Home / Explore / Reels links in Instagram's navigation.
  var HIDE_NAV_LINKS = true;

  // ---- where are we? ----------------------------------------------------------------------------
  // Returns one of: 'ok', 'feed', 'explore', 'reels', 'viewer' (post/story/reel opened from somewhere),
  // 'profile-tab' (the Reels / Tagged tab of a profile).
  function classify(pathname) {
    var path = pathname.replace(/\/+$/, '') + '/';
    if (path === '/') return 'feed';
    if (path.indexOf('/direct/') === 0) return 'ok';
    if (path.indexOf('/accounts/') === 0) return 'ok';       // settings, activity, follow requests
    if (path.indexOf('/explore/search/') === 0) return 'ok'; // people search on mobile web
    if (path.indexOf('/explore/') === 0) return 'explore';
    if (path.indexOf('/reels/') === 0) return 'reels';
    if (path.indexOf('/reel/') === 0) return 'viewer';
    if (path.indexOf('/stories/') === 0) return 'viewer';
    if (path.indexOf('/p/') === 0) return BLOCK_SINGLE_POSTS ? 'viewer' : 'ok';
    var seg = path.split('/').filter(Boolean);
    if (seg.length >= 2 && (seg[1] === 'reels' || seg[1] === 'tagged' || seg[1] === 'channel')) {
      return 'profile-tab';
    }
    return 'ok'; // profiles, followers, following, and anything we do not know
  }

  // ---- act ----------------------------------------------------------------------------------------
  var lastPath = null;
  var lastActionAt = 0;

  function goToInbox() {
    window.location.replace(INBOX);
  }

  function check() {
    var path = window.location.pathname;
    var kind = classify(path);
    if (document.documentElement) {
      document.documentElement.classList.toggle('dmonly-blocked', kind !== 'ok');
    }
    if (kind === 'ok') {
      lastPath = path;
      return;
    }
    var now = Date.now();
    if (now - lastActionAt < 800) return; // let the previous redirect land
    lastActionAt = now;

    if (kind === 'viewer') {
      // Opened from a chat or a profile: go back there. If there is nowhere to go back to, use the inbox.
      if (window.history.length > 1 && lastPath !== null) {
        window.history.back();
      } else {
        goToInbox();
      }
    } else if (kind === 'profile-tab') {
      // Reels/Tagged tab of a profile: stay on the profile's main page.
      var user = path.split('/').filter(Boolean)[0];
      window.location.replace('/' + user + '/');
    } else {
      goToInbox();
    }
  }

  // ---- watch the address ----------------------------------------------------------------------------
  function hook(name) {
    var original = window.history[name];
    if (typeof original !== 'function') return;
    window.history[name] = function () {
      var result = original.apply(this, arguments);
      setTimeout(check, 0);
      return result;
    };
  }
  hook('pushState');
  hook('replaceState');
  window.addEventListener('popstate', function () { setTimeout(check, 0); });
  setInterval(check, 400);

  // ---- hide what we can before it renders ------------------------------------------------------------
  var css = '' +
    'html.dmonly-blocked main, html.dmonly-blocked section, html.dmonly-blocked article, ' +
    'html.dmonly-blocked [role="main"] { visibility: hidden !important; }';
  if (HIDE_NAV_LINKS) {
    css += 'a[href="/"], a[href="/explore/"], a[href="/reels/"], a[href^="/reels/"], ' +
      'a[href$="/reels/"], a[href$="/tagged/"] { display: none !important; }';
  }
  // At document-start the document can still be empty, so keep trying until there is somewhere to put the style.
  function injectStyle() {
    if (document.getElementById('dmonly-style')) return;
    var parent = document.head || document.documentElement;
    if (!parent) { setTimeout(injectStyle, 10); return; }
    var style = document.createElement('style');
    style.id = 'dmonly-style';
    style.textContent = css;
    parent.appendChild(style);
  }
  injectStyle();
  document.addEventListener('DOMContentLoaded', injectStyle);

  check();
})();
