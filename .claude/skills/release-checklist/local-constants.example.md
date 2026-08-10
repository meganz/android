# release-checklist — local constants (TEMPLATE)

> **Copy this file to `local-constants.md`** (same folder) and fill in the real
> values, then use that copy. `local-constants.md` is **gitignored** on purpose:
> it holds MEGA-internal infrastructure identifiers (Slack channel / user-group
> IDs, Jira epic + custom fields, Confluence page ids, TestRail project id) that
> must **not** be committed to this repo, which mirrors publicly.
>
> `SKILL.md` refers to every one of these by **name** (e.g. `#devops-cicd`,
> `@eu-mobile-release`). At runtime, resolve the name to its real value from your
> filled-in `local-constants.md`.
>
> **Where to find each value:**
> - Slack channel/user-group/bot IDs → open the channel/group in Slack (⌄ → *Copy link*,
>   the `C…`/`S…`/`U…` id is in the URL), or `slack_search_channels` / `slack_search_users`.
> - Jira epic, custom fields, transition id → the Confluence *Android Secure Release
>   Checklist* page (Prerequisite section) and a sample RC ticket (`jira_get_create_fields`,
>   `jira_get_transitions`).
> - Confluence page id → the *Android Release plan - <year>* page URL / page-info.
> - TestRail project id → the TestRail project URL.

## Slack channels
| Name | ID |
|---|---|
| `#mobile-platform` | `C________` |
| `#mobile-dev-team` | `C________` |
| `#android-dev-team` | `C________` |
| `#sdk` | `C________` |
| `#sdk-android-pipeline` | `C________` |
| `#megachat_native` | `C________` |
| `#devops-cicd` | `C________` |
| `#android` | `C________` |
| `#release` | `C________` |
| `#app_release_updates` | `C________` |

## Slack user groups (`<!subteam^ID>`)
| Name | ID |
|---|---|
| `@productmanagers` | `S________` |
| `@androiddevs-urgent` | `S________` |
| `@eu-mobile-release` | `S________` |
| `@nz-mobile-release` | `S________` |

## Slack users / bots
| Name | ID |
|---|---|
| Release announcement bot | `U________` |

## Jira
| Thing | Value |
|---|---|
| RC ticket Epic | `AND-_____` |
| Expense Product field | `customfield_______` → value `Cloud/Default` |
| Expense Type field | `customfield_______` → value `OPEX` |
| "Start Progress" transition id | `___` |

## Confluence
| Thing | Value |
|---|---|
| Android Release plan (current year) page id | `_________` |

## TestRail
| Thing | Value |
|---|---|
| Android project id | `_` |
