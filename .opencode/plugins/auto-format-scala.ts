import { Plugin } from "@opencode/plugin"
import { execFile } from "node:child_process"
import { promisify } from "node:util"

const execFileAsync = promisify(execFile)

/**
 * Runs `scalafmt` on any .scala file right after the agent edits or writes
 * it, using the project's .scalafmt.conf. Keeps formatting diffs from
 * piling up between explicit `scalafmtAll` passes.
 *
 * Requires the `scalafmt` CLI to be on PATH (installed via coursier/cs).
 */
export default Plugin.define({
  id: "auto-format-scala",
  async setup(ctx) {
    await ctx.tool.hook("execute.after", async (event) => {
      if (event.tool !== "edit" && event.tool !== "write") return

      const filePath = (event.input as { filePath?: string } | undefined)?.filePath
      if (!filePath || !filePath.endsWith(".scala")) return

      try {
        await execFileAsync("scalafmt", [filePath])
      } catch (err) {
        console.error(`[auto-format-scala] scalafmt failed for ${filePath}:`, err)
      }
    })
  },
})
