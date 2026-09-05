#!/bin/sh
# Render alertmanager.yml from template using container env vars (set via env_file),
# then exec alertmanager. Kept OUTSIDE docker-compose so compose interpolation
# never touches the ${SMTP_*} placeholders.
set -e

render_template() {
  key="$1"
  case "$key" in
    SMTP_SERVER) pattern='\${SMTP_SERVER}' ;;
    SMTP_PORT)   pattern='\${SMTP_PORT}' ;;
    SMTP_USER)   pattern='\${SMTP_USER}' ;;
    SMTP_PASS)   pattern='\${SMTP_PASS}' ;;
    *) echo "unsupported template variable: $key" >&2; exit 1 ;;
  esac

  value=$(printenv "$key" 2>/dev/null || true)
  # Escape sed replacement metacharacters: backslash, ampersand, delimiter.
  escaped=$(printf '%s' "$value" | sed 's/[\\&|]/\\&/g')
  sed "s|$pattern|$escaped|g"
}

render_template SMTP_SERVER < /etc/alertmanager/alertmanager.yml.template \
  | render_template SMTP_PORT \
  | render_template SMTP_USER \
  | render_template SMTP_PASS \
  > /etc/alertmanager/alertmanager.yml

exec /bin/alertmanager --config.file=/etc/alertmanager/alertmanager.yml
