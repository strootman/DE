package org.iplantc.de.theme.base.client.diskResource.grid;

import com.google.gwt.i18n.client.Messages;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Created by jstroot on 1/26/15.
 * @author jstroot
 */
public interface GridViewDisplayStrings extends Messages{

    @Key("diskResourceNotAvailable")
    String diskResourceNotAvailable();

    @Key("markFavoriteError")
    String markFavoriteError();

    @Key("metadata")
    String metadata();

    @Key("metadataFormInvalid")
    String metadataFormInvalid();

    @Key("metadataHelp")
    String metadataHelp();

    @Key("noItemsToDisplay")
    String noItemsToDisplay();

    @Key("pathFieldEmptyText")
    String pathFieldEmptyText();

    @Key("pathFieldLabel")
    SafeHtml pathFieldLabel();

    @Key("removeFavoriteError")
    String removeFavoriteError();

    @Key("share")
    String share();

    @Key("viaDiscoveryEnvironment")
    String viaDiscoveryEnvironment();

    @Key("viaPublicLink")
    String viaPublicLink();
}
