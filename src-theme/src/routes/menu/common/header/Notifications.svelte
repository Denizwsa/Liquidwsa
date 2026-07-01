<script lang="ts">
    import {fly} from "svelte/transition";
    import {notification, type TNotification} from "./notification_store";
    import {onMount} from "svelte";

    interface NotificationWithId {
        notification: TNotification;
        id: number;
    }

    let notifications: NotificationWithId[] = [];

    onMount(() => {
       notifications = [];
    });

    notification.subscribe((v) => {
        if (!v) {
            return;
        }
        const id = Date.now();
        const n = {
            notification: v,
            id
        };
        notifications = [...notifications, n];
        setTimeout(() => {
            notifications = notifications.filter(n => n.id !== id);
        }, (v?.delay ?? 3) * 1000);
    });
</script>

<div class="notifications">
    {#each notifications as n (n.id)}
        <div class="notification" transition:fly|global={{duration: 500, y: -100}}>
            <div class="icon" class:error={n.notification.error}>
                <img src="img/hud/notification/icon-info.svg" alt="info">
            </div>
            <div class="title">{n.notification.title}</div>
            <div class="message">{n.notification.message}</div>
        </div>
    {/each}
</div>

<style lang="scss">

  .notifications {
    display: grid;
    grid-template-columns: 1fr;
  }

  .notification {
    grid-row-start: 1;
    grid-column-start: 1;
    background-color: var(--menu-header-notification-background-color);
    border-radius: 5px;
    display: grid;
    grid-template-areas:
        "a b"
        "a c";
    grid-template-columns: max-content 1fr;
    overflow: hidden;
    padding-right: 8px;
    min-width: 280px;
    max-width: 360px;

    .title {
      color: var(--menu-text-color);
      font-weight: 600;
      font-size: 15px;
      grid-area: b;
      align-self: flex-end;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .message {
      color: var(--menu-text-dimmed-color);
      font-weight: 500;
      font-size: 12px;
      grid-area: c;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .icon {
      grid-area: a;
      height: 52px;
      width: 52px;
      background-color: var(--menu-header-notification-icon-background-color);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 8px;

      &.error {
        background-color: var(--menu-header-notification-icon-error-background-color);
      }
    }
  }
</style>
