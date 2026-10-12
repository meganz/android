# Code Comment Conventions

The Documentation rules in `CLAUDE.md` decide **whether** to write a comment: comment why, not
what, and match the comment density of the file. This file decides **how** to write a comment
after you decide that it is necessary.

Many readers of this code are not native English speakers, but all of them are experienced
Android engineers. Apply the core principles of ASD-STE100 Simplified Technical English. Do
**not** aim for strict ASD-STE100 compliance — **technical accuracy, clarity and natural
readability come first.**

## Scope

- **Apply to** the KDoc and `//` / `/* */` comments that you write or change, including test code
  and `TODO` text.
- **Do not apply to** code, identifiers, `@param` / `@property` names, URLs, quoted log messages
  or UI strings, license headers and generated code. Keep that text exactly as it is.
- **Do not rewrite existing comments** only to make them follow these rules. Change a comment only
  when you change the code it describes, or when the comment is wrong.

## Rules

- **Consistent terminology (strict).** Use the same term as the code. If the parameter is
  `nodeId`, do not call it "handle" in the comment. Use one term for one thing, and do not use one
  word for two different things. Refer to symbols as `[NodeId]` in KDoc and with backticks in
  `//` comments. This is the one rule to enforce strictly.
- **Sentences.** Prefer short sentences — about 15–20 words is a good target, not a hard limit.
  Express one main idea per sentence, but keep closely related ideas together. Do not split one
  explanation into many short, disconnected sentences.
- **State the reason.** A comment exists to explain why, so make the cause and the effect
  explicit with "because", "so that", "if" or "otherwise". Name the real consequence. Do not write
  vague reasons such as "to avoid issues", "for safety" or "just in case".
- **Words.** Prefer familiar words. Avoid idioms, slang, contractions and phrasal verbs that a
  non-native reader can misread — write "stop", not "bail out"; "start", not "kick off"; "do not",
  not "don't". Avoid "under the hood", "hack around", "nuke" and "piggyback".
- **Precise verbs.** Avoid the verbs "handle", "deal with", "take care of", "process" and
  "manage" when a precise verb exists ("retries", "ignores", "maps", "caches"). This rule does not
  apply to the nouns, for example a node handle or the app process. Avoid "properly", "correctly"
  and "stuff".
- **Clear references.** Do not start a comment with "this", "it" or "that" when the reader cannot
  tell which thing the word refers to. Name the thing.
- **Requirements.** Use "must" and "must not" for a requirement ("Must be called on the main
  thread."). Do not use "should" for a hard rule.
- **Voice and tense.** Prefer the active voice and the present tense. Use the passive when the doer
  is unknown or not relevant.
- **Articles.** Keep "the" and "a" in sentences — no telegraphic style. A short trailing comment
  of a few words can be a phrase.
- **Concrete values.** Give the unit for every time and size value ("in milliseconds", "in
  bytes"). When a value comes from a design spec or an SDK contract, say so.
- **Keep standard engineering terminology.** Android, Kotlin, Gradle and MEGA terms are the most
  precise words for the reader. Keep them exactly. Do not simplify them, rename them or expand
  their abbreviations — e.g. coroutines, lifecycle, `collect`, `emit`, `recompose`, SDK, R8, FCM,
  ANR, Cloud Drive, Camera Uploads, Rubbish Bin.
- **Do not oversimplify.** Explain a non-obvious decision or trade-off with the precision it needs,
  even when that takes a longer sentence.

## KDoc

- Start with one summary sentence in the third person and the present tense: "Returns …",
  "Monitors …", "Maps …".
- Add `@param` and `@return` text only when it says more than the name. Do not write
  "`@return` the result".
- Describe the behavior that a caller must know: the threading, the exceptions that the function
  throws, and what happens on `null` or on an empty list.

## Examples

Reason and words:

```kotlin
// ❌ Kick off the sync here, otherwise the worker might bail out early.
// ✅ Enqueue the sync before the worker starts because the worker stops at launch if its queue
//    is empty, and the sync then does not run.
```

Terminology — the parameter is `nodeId`, so the comment uses `nodeId`:

```kotlin
// ❌ Get the handle first so the folder stuff is dealt with properly.
// ✅ Check that `nodeId` still resolves to a node before the move because the SDK fails a move
//    into a deleted folder.
```

KDoc:

```kotlin
/** ❌ Handles node fetching, taking care of caching under the hood. */

/**
 * ✅ Returns the child nodes of [parentId]. Reads the cache first and calls the SDK only on a
 * cache miss.
 */
```

Concrete values:

```kotlin
// ❌ Wait a bit before showing the progress bar.
// ✅ Wait 500 ms (the delay in the design spec) before showing the progress bar, so that a fast
//    transfer does not make the bar flash.
```

Over-simplified — the trade-off needs a longer sentence:

```kotlin
// ❌ Use Paging because the list is big.
// ✅ Load the children with Paging because a Cloud Drive folder can have thousands of nodes, and
//    mapping all of them before the first frame makes the screen open slowly.
```
