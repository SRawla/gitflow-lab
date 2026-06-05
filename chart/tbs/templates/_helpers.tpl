{{/*
Expand the name of the chart.
*/}}
{{- define "tbs.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Full release name, capped at 63 chars.
*/}}
{{- define "tbs.fullname" -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "tbs.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/name: {{ include "tbs.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "tbs.selectorLabels" -}}
app.kubernetes.io/name: {{ include "tbs.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Full image reference: registry/repository:tag
*/}}
{{- define "tbs.image" -}}
{{ .Values.image.registry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- end }}

{{/*
PostgreSQL service name (Bitnami sub-chart convention)
*/}}
{{- define "tbs.postgresHost" -}}
{{- printf "%s-postgresql" .Release.Name }}
{{- end }}
