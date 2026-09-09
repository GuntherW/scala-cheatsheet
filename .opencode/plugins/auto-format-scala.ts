import type { Plugin } from "@opencode-ai/plugin"

/**
 * Runs `scalafmt` on any .scala file right after the agent edits or writes
 * it, using the project's .scalafmt.conf. Keeps formatting diffs from
 * piling up between explicit `scalafmtAll` passes.
 *
 * Requires the `scalafmt` CLI to be on PATH (installed via coursier/cs).
 */
export const AutoFormatScala: Plugin = async ({ $ }) => {
  return {
    "tool.execute.after": async (input, output) => {
      if (input.tool !== "edit" && input.tool !== "write") return

      const filePath = input.args?.filePath as string | undefined
      if (!filePath || !filePath.endsWith(".scala")) return

      try {
        await $`scalafmt ${filePath}`.quiet()
      } catch (err) {
        console.error(`[auto-format-scala] scalafmt failed for ${filePath}:`, err)
      }
    },
  }
}
