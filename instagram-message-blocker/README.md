# DM Only — Instagram messages without the feed

A tiny Android app (no dependencies, ~30 KB) that lets you use Instagram **for messages and
people, not for scrolling**. Whenever the home feed, Reels, the Explore grid or a story shows up,
it sends you straight back to your inbox. Chats, message requests, follow requests, profiles,
following people and searching for people all work normally.

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

| Screen in Instagram                                        | Result                                    |
| ---------------------------------------------------------- | ----------------------------------------- |
| Inbox, chats, message requests, notes                      | Allowed                                   |
| Profiles, follow / unfollow, followers and following lists | Allowed                                   |
| Follow requests, notifications                             | Allowed                                   |
| Searching for people (the results list)                    | Allowed                                   |
| Home feed, Reels tab, Explore grid                         | Jump to the inbox (configurable)          |
| A reel or story opened from a chat or profile              | Back to where you were                    |
| Opening a single post from a profile                       | Blocked (it uses the feed layout); the profile grid itself is visible |
| Backing out of messages onto the feed                      | Leaves Instagram (goes to the home screen), so you are not bounced back into the inbox forever. Switchable. |

Rules come in two lists. *Always allowed* rules win over *blocked* rules, so a profile stays
reachable even when it is stacked on top of the blocked feed. Screens matching neither list are
allowed unless strict mode is on.

- **Pause for 15 minutes** when you genuinely need settings or a profile.
- **Strict mode** blocks every screen that does not match an *always allowed* rule. More thorough,
  but it depends on the inbox, profiles and so on being recognised, so try it only after the
  inspector shows those screens as "allowed by:" a rule rather than "no rule matched".
- Action on a blocked screen: *jump to inbox* (default), *press Back*, or *go to home screen*.
- **Loop guard:** if the app has to act 6 times within 12 seconds it assumes it cannot recognise the
  inbox, goes to the home screen and pauses itself for 30 seconds instead of fighting you.

## When Instagram changes (keeping the rules working)

Instagram renames its internal view ids every few months. The rules are therefore editable in the
app and there is a **Screen inspector** at the bottom that shows the ids and content descriptions
the service saw on the last Instagram screen, plus the last decisions.

If a screen that should be blocked is not, or a screen you need gets blocked:

1. Open that screen in Instagram, switch back to DM Only, tap **Refresh**. The first lines say
   which rule decided ("blocked by: row_feed_", "allowed by: profile_header", "no rule matched").
2. Pick an id that is specific to that screen from the list (for the feed it will be something like
   `row_feed_...`; for Reels `clips_viewer_...`; for a profile `profile_header...`).
3. Add it to the *blocked* list, or to the *always allowed* list for a screen you need, and tap
   **Save rules**.

Rule syntax, one per line:

```
clips_viewer              # substring of a view id, case-insensitive
clips_tab [selected]      # the same, but only while that bottom tab is the active one
desc:search and explore   # match a content description instead of an id
clips_viewer -> back      # override the action for this screen: inbox, back or home
# comments start with #
```

Always-allowed rules win over blocked rules; within a list the first match decides.

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
