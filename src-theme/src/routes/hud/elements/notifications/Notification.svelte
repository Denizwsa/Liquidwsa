<script lang="ts">
    export let title: string;
    export let message: string;
    export let severity: string;
</script>

<div class="notification">
    <div class="icon {severity.toString().toLowerCase()}"></div>
    <div class="title">{title}</div>
    <div class="message">{message}</div>
</div>

<style lang="scss">
  .notification {
    display: grid;
    grid-template-areas:
            "a b"
            "a c";
    grid-template-columns: max-content 1fr;
    column-gap: 8px;
    background: var(--notification-background-color);
    border-radius: 5px;
    width: 260px;
    overflow: hidden;
    padding: 8px;
  }

  .icon {
    height: 36px;
    width: 36px;
    background-position: center;
    background-repeat: no-repeat;
    border-radius: 4px;
    grid-area: a;
    transition: background-color 0.2s;
    position: relative;
    background-image: url("/img/hud/notification/icon-toggle.svg");

    &.success {
      background-color: var(--notification-success-color);
      background-image: url("/img/hud/notification/icon-success.svg");
    }

    &.error {
      background-color: var(--notification-error-color);
      background-image: url("/img/hud/notification/icon-error.svg");
    }

    &.info {
      background-color: var(--notification-info-color);
      background-image: url("/img/hud/notification/icon-info.svg");
    }

    &.disabled,
    &.enabled {
      &::after {
        content: "";
        position: absolute;
        height: 9px;
        width: 9px;
        border-radius: 5px;
        top: 50%;
        transform: translate(-50%, -50%);
        background: var(--notification-toggle-knob-color);
        transition: all 0.2s ease-out;
      }
    }

    &.enabled {
      background-color: var(--notification-success-color);

      &::after {
        left: 62%;
      }
    }

    &.disabled {
      background-color: var(--notification-error-color);

      &::after {
        left: 38%;
      }
    }
  }

  .title {
    grid-area: b;
    font-size: 13px;
    color: var(--notification-title-color);
    font-weight: 600;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .message {
    grid-area: c;
    font-size: 11px;
    color: var(--notification-message-color);
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
</style>
