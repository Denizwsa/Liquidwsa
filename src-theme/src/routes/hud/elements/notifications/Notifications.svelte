<script lang="ts">
    import {flip} from "svelte/animate";
    import {listen} from "../../../../integration/ws";
    import {fly, scale} from "svelte/transition";
    import Notification from "./Notification.svelte";
    import type {NotificationEvent} from "../../../../integration/events";

    const MAX_VISIBLE = 4;
    const STACK_SPACING = 10;
    const STACKED_OFFSET = 6;
    const OVERLAP_THRESHOLD = 4;
    const VISIBLE_DURATION = 3000;
    const ANIMATION_SPEED = 240;

    interface TNotification {
        animationKey: number;
        id: number;
        title: string;
        severity: string;
        message: string;
    }

    let notifications: TNotification[] = [];
    $: visibleNotifications = notifications.slice(0, MAX_VISIBLE);
    $: denseStack = visibleNotifications.length >= OVERLAP_THRESHOLD;
    $: itemSpacing = denseStack ? STACKED_OFFSET : 52 + STACK_SPACING;

    function addNotification(title: string, message: string, severity: string) {
        let animationKey = Date.now();
        const id = animationKey;

        if (severity === "ENABLED" || severity === "DISABLED") {
            // Check if there still exists an enable/disable notification for the same module
            const index = notifications.findIndex((n) => n.message === message)
            if (index !== -1) {
                // Set the id of the new notification to the old notification's id.
                // This will make svelte able to animate it correctly
                animationKey = notifications[index].animationKey;

                // Remove the old notification
                notifications.splice(index, 1);
            }
        }

        notifications = [
            {animationKey, id, title, message, severity},
            ...notifications,
        ].slice(0, MAX_VISIBLE * 2);
        
        setTimeout(() => {
            notifications = notifications.filter((n) => n.id !== id);
        }, VISIBLE_DURATION);
    }

    listen("notification", (e: NotificationEvent) => {
        addNotification(e.title, e.message, e.severity);
    });
</script>

<div class="notifications" style:--notification-step={`${itemSpacing}px`}>
    {#each visibleNotifications as {title, message, severity, animationKey}, index (animationKey)}
        <div
                class="notification-shell"
                style:z-index={visibleNotifications.length - index}
                class:dense={denseStack}
                animate:flip={{ duration: ANIMATION_SPEED }}
                in:scale={{ start: 0.94, duration: ANIMATION_SPEED }}
                out:fly={{ x: 30, duration: ANIMATION_SPEED }}
        >
            <Notification {title} {message} {severity}/>
        </div>
    {/each}
</div>

<style lang="scss">
  .notifications {
    display: flex;
    flex-direction: column;
    gap: 0;
  }

  .notification-shell {
    height: var(--notification-step);
    transform-origin: right center;
    will-change: transform, opacity;

    &.dense {
      filter: drop-shadow(0 3px 8px color-mix(in srgb, black 22%, transparent));
    }
  }
</style>
