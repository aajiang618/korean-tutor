# DM Only — Instagram messages without the feed

A tiny Android app (no dependencies, ~30 KB) that lets you use Instagram **only for messages**.
Whenever the feed, Reels, Explore, stories or a profile page shows up, it sends you straight back
to your inbox. Chats, message requests and the inbox itself work normally.

It is an [accessibility service](https://developer.android.com/guide/topics/ui/accessibility/service):
it reads the view ids of the screen Instagram is showing, compares them with a small rule list, and
reacts. Nothing leaves the phone, there is no network permission at all.

> **iPhone:** iOS does not let a third-party app hide parts of another app, so this cannot be
> ported. See [iPhone options](#iphone-options) at the bottom for what does work there.

## Install (prebuilt APK)

1. On the phone, download [`dist/dmonly-debug.apk`](dist/dmonly-debug.apk) and open it. Allow
   "install from this source" when asked. Android will warn that the app is not from Play — that is
   expected for a sideloaded app.
2. Open **DM Only** and tap **Open accessibility settings**. Find *DM Only for Instagram* under
   *Installed apps / Downloaded apps* and switch it on.
3. **Android 13 or newer:** the switch is greyed out with a *Restricted setting* message the first
   time. Go to *Settings › Apps › DM Only › ⋮ (top right) › Allow restricted settings*, then repeat
   step 2. This is Android's standard guard for sideloaded accessibility services.
4. Open Instagram. You should land in the inbox instead of the feed. Tap **Test: open Instagram
   inbox** in DM Only if you want to confirm the inbox link works on your Instagram version.

Repeat on each Android phone. Uninstall or switch the service off any time; the app has no lock.

## What it does exactly

| Screen in Instagram                                   | Result                                    |
| ----------------------------------------------------- | ----------------------------------------- |
| Inbox, chats, message requests, notes in the inbox    | Allowed                                   |
| Feed, Explore/Search, Reels tab, Profile tab, Create  | Jump to the inbox (configurable)          |
| A reel or story opened from a chat                    | Back to the chat                          |
| Backing out of messages onto the feed                 | Leaves Instagram (goes to the home screen), so you are not bounced back into the inbox forever. Switchable. |

- **Pause for 15 minutes** when you genuinely need settings or a profile.
- **Strict mode** flips the logic: every screen that is not recognised as messages is blocked. More
  thorough, but it depends on the inbox being recognised, so try it only after the inspector shows
  the inbox ids matching an allow pattern.
- Action on a blocked screen: *jump to inbox* (default), *press Back*, or *go to home screen*.
- **Loop guard:** if the app has to act 6 times within 12 seconds it assumes it cannot recognise the
  inbox, goes to the home screen and pauses itself for 30 seconds instead of fighting you.

## When Instagram changes (keeping the rules working)

Instagram renames its internal view ids every few months. The rules are therefore editable in the
app and there is a **Screen inspector** at the bottom that shows the ids and content descriptions
the service saw on the last Instagram screen, plus the last decisions.

If a screen that should be blocked is not (or messages get blocked):

1. Open that screen in Instagram, switch back to DM Only, tap **Refresh**.
2. Pick an id that is specific to that screen from the list (for the feed it will be something like
   `feed_tab`; for Reels `clips_viewer_...`).
3. Add it to the block list (or the allow list for message screens) and tap **Save rules**.

Rule syntax, one per line:

```
clips_tab                 # substring of a view id, case-insensitive
desc:search and explore   # match a content description instead of an id
clips_viewer -> back      # override the action for this screen: inbox, back or home
# comments start with #
```

Block rules win over allow rules; the first matching block rule decides the action.

## Building it yourself

**Android Studio:** open the `instagram-message-blocker` folder, let it sync, *Run*. The project is
plain Java with no libraries. Both build types sign with `debug.keystore` from this folder, so a
Studio build installs over the prebuilt APK (same key). That key is public and for personal use only.

**Without Android Studio or the SDK:** `tools/build-without-android-studio.sh` needs only a JDK 17+,
curl, unzip and zip. It downloads `android.jar`, `aapt2` (from the Apktool jar), `dx` and
`uber-apk-signer` into `tools/.cache` on first run and writes `app/build/manual/dmonly-debug.apk`.
This is how `dist/dmonly-debug.apk` was produced.

## Limitations

- Instagram in a **browser** is not touched. If that is a loophole for you, block
  `instagram.com` in the browser or with a DNS filter and keep the app for messages.
- The service is only as good as the id rules; see the section above when Instagram updates.
- It relies on Instagram accepting `https://www.instagram.com/direct/inbox/` as a deep link to the
  inbox. If that stops working, switch the action to *press Back* or change the link in the app.
- Nothing stops you from switching the service off. If you want friction, pair it with a Digital
  Wellbeing app timer on DM Only itself, or hand the accessibility toggle to a device-admin style
  app locker.

## iPhone options

iOS has no equivalent of accessibility services for third-party apps, so an app cannot hide the feed
inside Instagram. What does work:

- **Screen Time:** put an app limit on Instagram and read messages at
  `instagram.com/direct/inbox/` in Safari. Add *instagram.com/explore* and *instagram.com/reels* to
  Screen Time › Content Restrictions › Never Allow websites.
- **Shortcuts automation:** *When Instagram is opened › Open URL* `https://www.instagram.com/direct/inbox/`.
  Every launch then lands in the inbox instead of the feed (it does not stop you tapping Reels
  afterwards).
- **Instagram's own settings:** *Settings › Time management* has a daily limit and quiet mode.
