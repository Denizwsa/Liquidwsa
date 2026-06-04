<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {mapToColor} from "../../../../util/color_utils";
    import {itemTextureUrl} from "../../../../integration/rest";

    export let stack: ItemStack;

    const {count, damage, identifier, maxDamage, enchantments} = stack;

    const countColor = count <= 0 ? "red" : "white";

    const valueColor = mapToColor(120 * (maxDamage - damage) / maxDamage);
</script>

<div class="item-stack">
    <img class="item-icon" src={itemTextureUrl(identifier)} alt={identifier}/>

    <div class="durability-bar" class:hidden={damage === 0}>
        <div class="durability"
             style="width: {100 * (maxDamage - damage) / maxDamage}%; background-color: {valueColor}">
        </div>
    </div>

    <div class="count" class:hidden={count === 1 || identifier === "minecraft:air"} style="color: {countColor}">
        {count}
    </div>
</div>

<style lang="scss">

  .hidden {
    display: none;
  }

    .item-stack {
    position: relative;
    width: 32px;
    height: 32px;
  }

  .item-icon {
    width: 100%;
    height: 100%;
  }

  .durability-bar {
    position: absolute;
    bottom: 0;
    left: 10%;
    width: 80%;
    height: 2px;
    background-color: var(--item-damage-background-color);
  }

  .durability {
    height: 100%;
    transition: width 150ms;
  }

  .count {
    position: absolute;
    bottom: 0;
    right: 0;
    font-size: 14px;
    font-weight: bold;
    font-family: monospace;
  }
</style>
