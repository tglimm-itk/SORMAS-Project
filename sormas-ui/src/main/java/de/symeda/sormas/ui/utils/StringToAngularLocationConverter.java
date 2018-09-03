package de.symeda.sormas.ui.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import com.vaadin.data.util.converter.StringToDoubleConverter;

public final class StringToAngularLocationConverter extends StringToDoubleConverter {
	
	protected NumberFormat getFormat(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
		
		DecimalFormat numberFormat = (DecimalFormat)NumberFormat.getNumberInstance(locale);
		numberFormat.setGroupingUsed(false);
		numberFormat.setMaximumFractionDigits(5);

		return numberFormat;
	}
	
}