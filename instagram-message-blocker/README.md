# DM Only — Instagram messages without the feed

A tiny Android app (no dependencies, ~30 KB) that lets you use Instagram **for messages and
people, not for scrolling**. Whenever the home feed, Reels, the Explore grid or a story shows up,
it sends you straight back to your inbox. Chats, message requests, follow requests, profiles,
following people and searching for people all work normally.

It is an [accessibility service](https://developer.android.com/guide/topics/ui/accessibility/service):
it reads the view ids of the screen Instagram is showing, compares them with a small rule list, and
reacts. Nothing leaves the phone, there is no network permission at all.

> **iPhone:** iOS does not let a third-party app hide parts of another app, so the Android app
> cannot be ported. The [iPhone](#iphone) section has the Safari version instead: same idea, works
> on instagram.com.

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

## iPhone

iOS has no equivalent of accessibility services for third-party apps, so nothing can hide the feed
inside the Instagram *app*. Instagram's website has full messaging though, and Safari on iPhone can
run user scripts. `ios/dm-only.user.js` is that script. On instagram.com it hides the Home, Explore
and Reels links, sends the feed, Explore and the Reels tab to the inbox, and sends a post, story or
reel you open back to where you came from. To feel like an app rather than a web page it also blanks
a blocked screen before it can flash, removes Safari's "Open in the app" banner, and lets the Back
button leave Instagram instead of looping. Profiles, followers lists, follow requests, notifications
and people search all keep working.

Setup, about five minutes, no computer needed:

1. Install **Userscripts** from the App Store (free, open source, by Justin Wasack).
2. *Settings › Apps › Safari › Extensions › Userscripts*: turn it on and set *instagram.com* (or
   *All websites*) to **Allow**.
3. Open Userscripts once and tap *Set Userscripts Directory* to pick a folder (any folder is fine).
4. In Safari, open
   `https://raw.githubusercontent.com/aajiang618/korean-tutor/main/instagram-message-blocker/ios/dm-only.user.js`
   (until this branch is merged, replace `main` with `claude/instagram-message-blocker-qi6xtq`).
   Tap the extensions button in the address bar, choose *Userscripts*, and tap **Install**.
5. Open `instagram.com` in Safari and log in. You should land in the inbox with no Home or Reels
   links.
6. **Make it feel like an app with a Home Screen icon.** In the **Shortcuts** app, tap **+**, tap
   **Add Action**, search **Open URLs**, and set the URL to
   `https://www.instagram.com/direct/inbox/`. Tap the shortcut name at the top to rename it (for
   example *Messages*) and tap the icon next to the name to pick a glyph and colour. Then, from the
   shortcut, choose **Add to Home Screen**. Open Instagram from this icon from now on.

   Why the Shortcut and not Safari's own *Add to Home Screen*: an icon made with Safari's *Add to
   Home Screen* runs as a stand-alone web app **without Safari extensions**, so the blocker would
   not load and the feed would come back. The Shortcut opens the page in real Safari, where the
   script runs.

   You leave this "app" the way you leave any app: swipe up to the Home Screen. Do not rely on the
   in-page Back arrow to exit; the script now lets Back pass out of Instagram instead of looping,
   but swiping up is cleaner.

7. Deal with the Instagram app: delete it, or block it in *Screen Time › App Limits* with a
   one-minute limit. Keeping it installed but unlimited defeats the point.

Trade-off: instagram.com in Safari does not send push notifications for new messages. If you need
them, keep the app installed under a Screen Time limit and let notifications through; open the
messages from the Safari shortcut.

**Cheaper option if you keep the app:** a Shortcuts automation that at least starts you in the
inbox. *Shortcuts › Automation › + › App › Instagram › Is Opened › Run Immediately*, with one action
*Open URLs* set to `instagram://direct/inbox` (if that does not open the inbox on your version,
use `https://www.instagram.com/direct/inbox/`). Every launch then lands in messages, but nothing
stops you tapping Reels afterwards.

The script also works on a computer with Tampermonkey or Violentmonkey: open the same URL and the
extension offers to install it. Two settings at the top of the file: `BLOCK_SINGLE_POSTS` (set to
false to allow opening posts from profiles) and `HIDE_NAV_LINKS`.
