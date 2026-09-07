// ==UserScript==
// @name         DM Only for Instagram
// @namespace    https://github.com/aajiang618/korean-tutor
// @version      1.1.0
// @description  Turns instagram.com in Safari into a messages-only app. Hides the feed, Reels, Explore and stories; keeps DMs, profiles, follow requests and people search. No feed flash, no "open the app" banner, back exits cleanly.
// @author       aajiang618
// @match        https://www.instagram.com/*
// @match        https://instagram.com/*
// @run-at       document-start
// @grant        none
// @downloadURL  https://raw.githubusercontent.com/aajiang618/korean-tutor/main/instagram-message-blocker/ios/dm-only.user.js
// @updateURL    https://raw.githubusercontent.com/aajiang618/korean-tutor/main/instagram-message-blocker/ios/dm-only.user.js
// ==/UserScript==

/*
 * Safari on iPhone (via the free "Userscripts" app), or any desktop browser with Tampermonkey.
 * See ../README.md, section "iPhone", for install and the Home Screen icon.
 */
(function () {
  'use strict';

  // ---- settings ---------------------------------------------------------------------------------
  var INBOX = '/direct/inbox/';
  var BLOCK_SINGLE_POSTS = true; // /p/... uses the feed layout; set false to allow opening posts.
  var HIDE_NAV_LINKS = true;     // Hide the Home / Explore / Reels links in Instagram's nav.
  var HIDE_WEB_NAGS = true;      // Remove the native "Open in the app" banner and similar web nags.

  // ---- where are we? ----------------------------------------------------------------------------
  // 'ok' | 'feed' | 'explore' | 'reels' | 'viewer' (post/story/reel) | 'profile-tab' (a profile's Reels/Tagged tab)
  function classify(pathname) {
    var path = pathname.replace(/\/+$/, '') + '/';
    if (path === '/') return 'feed';
    if (path.indexOf('/direct/') === 0) return 'ok';
    if (path.indexOf('/accounts/') === 0) return 'ok';       // settings, activity, follow requests
    if (path.indexOf('/explore/search/') === 0) return 'ok'; // people search
    if (path.indexOf('/explore/') === 0) return 'explore';
    if (path.indexOf('/reels/') === 0) return 'reels';
    if (path.indexOf('/reel/') === 0) return 'viewer';
    if (path.indexOf('/stories/') === 0) return 'viewer';
    if (path.indexOf('/p/') === 0) return BLOCK_SINGLE_POSTS ? 'viewer' : 'ok';
    var seg = path.split('/').filter(Boolean);
    if (seg.length >= 2 && (seg[1] === 'reels' || seg[1] === 'tagged' || seg[1] === 'channel')) {
      return 'profile-tab';
    }
    return 'ok'; // profiles, followers, following, notifications, and anything we do not recognise
  }

  // ---- a cover so a blocked screen never flashes before we move on ------------------------------
  function cover(show) {
    var el = document.getElementById('dmonly-cover');
    if (show) {
      if (!el) {
        el = document.createElement('div');
        el.id = 'dmonly-cover';
        (document.body || document.documentElement).appendChild(el);
      }
      el.style.display = 'block';
    } else if (el) {
      el.style.display = 'none';
    }
  }

  // ---- routing ----------------------------------------------------------------------------------
  var lastAllowed = null; // last path we settled on that was allowed
  var lastActionAt = 0;
  var viaPop = false;     // this navigation came from Back/Forward
  var backSkips = 0;      // consecutive auto-Back steps, bounded so we never loop forever

  function goToInbox() { window.location.replace(INBOX); }

  function check() {
    var path = window.location.pathname;
    var blocked = classify(path) !== 'ok';
    if (document.documentElement) {
      document.documentElement.classList.toggle('dmonly-blocked', blocked);
    }
    cover(blocked);
    if (!blocked) {
      lastAllowed = path;
      viaPop = false;
      backSkips = 0;
      return;
    }
    var now = Date.now();
    if (now - lastActionAt < 500) return; // let the previous move land
    lastActionAt = now;

    // Pressed Back/Forward onto a blocked page: pass straight through it (this is how you leave the
    // app) instead of bouncing to the inbox. Bounded so a weird history can't spin.
    if (viaPop) {
      viaPop = false;
      if (backSkips < 8 && window.history.length > 1) {
        backSkips++;
        window.history.back();
        return;
      }
      backSkips = 0;
      goToInbox();
      return;
    }

    // Forward navigation into a blocked area.
    backSkips = 0;
    var kind = classify(path);
    if (kind === 'viewer') {
      if (window.history.length > 1 && lastAllowed !== null) window.history.back();
      else goToInbox();
    } else if (kind === 'profile-tab') {
      window.location.replace('/' + path.split('/').filter(Boolean)[0] + '/');
    } else {
      goToInbox();
    }
  }

  // Instagram navigates without reloading, so watch every way the URL can change.
  function hook(name) {
    var original = window.history[name];
    if (typeof original !== 'function') return;
    window.history[name] = function () {
      var r = original.apply(this, arguments);
      viaPop = false;
      setTimeout(check, 0);
      return r;
    };
  }
  hook('pushState');
  hook('replaceState');
  window.addEventListener('popstate', function () { viaPop = true; setTimeout(check, 0); });
  setInterval(check, 400);

  // ---- styling: hide blocked content, the cover, nav links, and web nags ------------------------
  var css = '' +
    '#dmonly-cover { position: fixed; inset: 0; z-index: 2147483647; background: #fff; display: none; }' +
    '@media (prefers-color-scheme: dark) { #dmonly-cover { background: #000; } }' +
    'html.dmonly-blocked main, html.dmonly-blocked section, html.dmonly-blocked article, ' +
    'html.dmonly-blocked [role="main"] { visibility: hidden !important; }';
  if (HIDE_NAV_LINKS) {
    css += 'a[href="/"], a[href="/explore/"], a[href="/reels/"], a[href^="/reels/"], ' +
      'a[href$="/reels/"], a[href$="/tagged/"] { display: none !important; }';
  }
  if (HIDE_WEB_NAGS) {
    // The sticky "Log in / Sign up" and "Open the app" bars that scream "this is a website".
    css += '[class*="AppInstall" i], [data-testid*="app-install" i] { display: none !important; }';
  }

  function injectStyle() {
    if (document.getElementById('dmonly-style')) return;
    var parent = document.head || document.documentElement;
    if (!parent) { setTimeout(injectStyle, 10); return; }
    var style = document.createElement('style');
    style.id = 'dmonly-style';
    style.textContent = css;
    parent.appendChild(style);
  }

  function killSmartBanner() {
    if (!HIDE_WEB_NAGS) return;
    // Safari's native "Open in the App Store" banner comes from this meta tag; removing it hides the banner.
    var m = document.querySelector('meta[name="apple-itunes-app"]');
    if (m) m.parentNode.removeChild(m);
  }

  injectStyle();
  killSmartBanner();
  document.addEventListener('DOMContentLoaded', function () { injectStyle(); killSmartBanner(); });

  check();
})();
