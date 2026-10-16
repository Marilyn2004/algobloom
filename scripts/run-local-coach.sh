#!/bin/bash
# Run from any directory. The key stays in the calling shell environment, never printed or saved.
set -euo pipefail
coach_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -z "${COACH_OPENAI_API_KEY:-}" ]]; then
  echo 'No development API key is set in this terminal. Configure COACH_OPENAI_API_KEY first.' >&2
  exit 1
fi
if [[ -x /usr/libexec/java_home ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi
cd "$coach_root"
exec ./mvnw spring-boot:run '-Dspring-boot.run.arguments=--spring.profiles.active=coach --coach.demo=false'
