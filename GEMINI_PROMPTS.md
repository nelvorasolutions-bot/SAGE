# Making the real character (the fun part)

This is the same method the reel used, upgraded for a phone app. Two stages: first make one consistent character with Gemini, then make it move (four short loops) and export each as a transparent animated GIF or WebP. You need four files: walk, idle, wave, drink.

A note on tools. Gemini (nano banana / Gemini image) is great at creating and keeping a consistent character, but it makes still images, not smooth motion. For the actual movement you have two options, and both end in a transparent animated file:

- Option A (closest to the reel, simplest): make a short pose set with Gemini, then assemble those frames into a looping GIF in a free tool. Good for a cute, pixel or flat cartoon look.
- Option B (smoothest, uses your stack): make the character with Gemini, then animate it with Kling or Veo (image to video), then remove the background and export a GIF. Best if you want it to look truly alive.

Names to export at the end (drop into `app/src/main/assets`, replacing the placeholders):
`buddy_walk`, `buddy_idle`, `buddy_wave`, `buddy_drink` (each `.gif` or `.webp`, transparent background).

## Stage 1: create the character (Gemini)

If you want it to look like your friend, upload a clear, front facing photo of her first, then use this. If you want a generic cutie, skip the photo and just use the prompt.

Prompt:

"Turn this person into a friendly cartoon mascot character for a phone app. Full body, standing, facing the camera, arms relaxed at the sides. Simple clean cartoon style with bold outlines and flat colors, cute and warm, big friendly eyes, gentle smile. Keep her recognizable: same skin tone, same hairstyle, same general vibe. Put her in a comfy casual outfit (a soft aqua top works nicely). Center her in the frame with empty space around her. Plain flat background, no shadow on the ground. Whole body visible from head to shoes."

Then lock the design:

"Great. Remember this exact character design, colors, hairstyle, and outfit. I am going to ask for the same character in a few different poses. Keep her identical each time, only change the pose."

## Stage 2A: the four poses (Gemini, for Option A)

Ask for each pose as its own image. Same character, transparent or plain flat background you can remove later. Ask for a few frames per action so the motion reads.

Walk (for `buddy_walk`):
"Same character, side three quarter view, mid stride walking: one leg forward and one back, arms swinging naturally, a light bounce. Give me 4 images of the walk cycle: contact, passing, contact on the other foot, passing. Identical character and colors in all four. Plain flat background, full body, centered."

Idle (for `buddy_idle`):
"Same character, front view, standing still and relaxed, a soft happy expression. Give me 3 nearly identical images with a tiny difference: normal, chest slightly up as if breathing in, and one with eyes closed for a blink. Plain flat background, full body, centered."

Wave (for `buddy_wave`):
"Same character, front view, smiling wider, one arm raised waving hello. Give me 3 images: arm starting up, hand tilted left, hand tilted right, as if waving. Plain flat background, full body, centered."

Drink (for `buddy_drink`):
"Same character, front view, holding a clear glass of water. Give me 4 images: glass held at chest, glass raised near the mouth, tilting the glass up to sip, and lowering it with a happy satisfied smile. Plain flat background, full body, centered."

Then assemble each set into a looping animated GIF with a transparent background. Free tools that do this in the browser: ezgif.com (Effects, then "make transparent" on the background color, then GIF maker to set the frame timing and loop), or remove.bg on each frame first for clean edges, then combine. Aim for roughly 8 to 12 frames per second. Loop forever.

## Stage 2B: animate with Kling or Veo (Option B, smoothest)

Take the single Gemini character image and animate short clips. Keep each clip 2 to 3 seconds, plain or green background so it is easy to cut out.

Image to video prompts:

Walk: "This cartoon character walks in place, a smooth natural walk cycle, arms swinging gently, looping. Camera still. Keep her design and colors exactly the same."

Idle: "This cartoon character stands still and breathes gently, occasional soft blink, tiny idle sway. Camera still. Keep her exactly the same."

Wave: "This cartoon character smiles and waves hello with one hand, friendly and warm, looping. Camera still. Keep her exactly the same."

Drink: "This cartoon character raises a glass of water, takes a sip, then lowers it with a happy smile. Camera still. Keep her exactly the same."

Then remove the background and export a transparent GIF or WebP. Easiest paths: unscreen.com (video to transparent GIF), or drop the clip in CapCut, cut it to a clean loop, use the background removal, and export; if CapCut exports video, run it through ezgif.com (video to GIF) and set the transparency there. Keep files small (short loops, modest size) so the app stays snappy.

## Quick checklist before you drop them in

- Four files: walk, idle, wave, drink.
- Each has a see through background (you should see the checkerboard in the preview, not white).
- Each is portrait, full body, centered, extra space trimmed.
- Named exactly `buddy_walk`, `buddy_idle`, `buddy_wave`, `buddy_drink` with `.gif` or `.webp`.
- Placed in `app/src/main/assets`, old placeholders deleted.
- Rebuild and run.
