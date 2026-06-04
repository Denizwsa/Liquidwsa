<script lang="ts">
    import {onMount} from "svelte";
    import type {Module} from "../../../integration/types";
    import {getModules} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import {getTextWidth} from "../../../integration/text_measurement";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    export let settings: { [name: string]: any };

    const cSettings = settings as HudArrayListSettings;
    const TEXT_FONT = "500 14px Inter";

    const widthCache = new Map<string, number>();
    let enabledModules: Array<Module & { fullName: string; width: number }> = [];
    let pendingUpdate = false;

    function getCachedWidth(fullName: string): number {
        let width = widthCache.get(fullName);
        if (width === undefined) {
            width = getTextWidth(fullName, TEXT_FONT);
            widthCache.set(fullName, width);
        }
        return width;
    }

    async function flushUpdate() {
        pendingUpdate = false;
        const modules = await getModules();
        const items: Array<Module & { fullName: string; width: number }> = [];

        for (const module of modules) {
            if (!module.enabled || module.hidden) continue;

            const formattedName = $spaceSeperatedNames
                ? convertToSpacedString(module.name)
                : module.name;
            const fullName = module.tag == null || !cSettings.showTags
                ? formattedName
                : formattedName + " " + module.tag;

            items.push({
                ...module,
                fullName,
                width: getCachedWidth(fullName)
            });
        }

        items.sort((a, b) => cSettings.order === "Ascending"
            ? a.width - b.width
            : b.width - a.width);

        enabledModules = items;
    }

    function scheduleUpdate() {
        if (pendingUpdate) return;
        pendingUpdate = true;
        setTimeout(flushUpdate, 50);
    }

    spaceSeperatedNames.subscribe(scheduleUpdate);

    onMount(flushUpdate);

    listen("moduleToggle", scheduleUpdate);
    listen("refreshArrayList", scheduleUpdate);
</script>

<div class="arraylist">
    {#each enabledModules as { fullName } (fullName)}
        <div
                class="module"
                style={cSettings.itemAlignment === "Left" ? "margin-right: auto;" : "margin-left: auto;"}
        >
            {fullName}
        </div>
    {/each}
</div>

<style lang="scss">

  .module {
    background-color: var(--arraylist-background-color);
    color: var(--arraylist-text-color);
    font-size: 14px;
    border-radius: 4px 0 0 4px;
    padding: 5px 8px;
    border-left: solid 4px var(--arraylist-border-color);
    width: max-content;
    font-weight: 500;
  }
</style>
