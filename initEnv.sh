kubectx dev-gcp
kubectl exec --tty deployment/bidrag-statistikk printenv | grep -E 'AZURE_|_URL|SCOPE' | grep -v -e 'BIDRAG_FORSENDELSE_URL'  > src/test/resources/application-lokal-nais-secrets.properties
