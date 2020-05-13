# Open Issues

- Some configurations on ContentBuilderPlugins belong to TransportHandlerPlugins.
  I.e. endpoint configuration. Content builders should only provide the
  needed parametrization. For example the ContentAssetPagePlugin should only
  add ID and locale information, not the endpoint itself.
  
  Having content builders add endpoints is prone to error since additional plugins
  can overwrite this information and TransportHandlerPlugins then may use
  malformed endpoints.
  
  Also, API endpoints are always simply passed through from the content builder's
  OSGi configuration without alteration.
  
  Therefore, the TransportHandlerPlugins should know the required endpoints
  itself.
  
- Clean up old abstract service hierarchy. It is not supported by OSGi and
  leads to subtile problems.
  
- Substitute Felix annotation by OSGi annotations

- Clean up naming of OSGi configurations, e.g. host should be named host not
  endpoint.
  
- Clean up documentation.

- Library folders can only be added to content assets. The cannot be removed.

- Unpublishing a content asset for a specific locale will remove the entire
  asset, not just the requested language version.