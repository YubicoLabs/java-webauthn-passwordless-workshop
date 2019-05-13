# Clean Up
_Skip this step if you are using a local dev environment._

**To prevent your account from accruing additional charges, you should remove any resources that are no longer needed.**

## Remove the Azure Web App
1. In Azure Cloud Shell, run the **[az webapp delete](https://docs.microsoft.com/en-us/cli/azure/webapp?view=azure-cli-latest#az-webapp-delete)** command
    ```
    az webapp delete --name MyWebapp --resource-group MyResourceGroup
    ```
2. If you created a new resource group just for this workshop then delete it using the **[az group delete](https://docs.microsoft.com/en-us/cli/azure/group?view=azure-cli-latest#az-group-delete)** command
    ```
    az group delete -n MyResourceGroup
    ```

## Delete the Passwordless Workshop in Azure Cloud Shell
1. If you created a cloud shell storage just for this workshop then you can delete it
2. Open https://portal.azure.com
3. Go to Resource groups
4. Find the cloud-shell-storage resource group and select it
5. Click Delete Resource Group and follow the instructions