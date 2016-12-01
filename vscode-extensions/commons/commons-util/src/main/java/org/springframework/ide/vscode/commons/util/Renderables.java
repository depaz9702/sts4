/*******************************************************************************
 * Copyright (c) 2016 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.commons.util;

import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.overzealous.remark.Remark;

/**
 * Static methods and convenience constants for creating some 'description
 * providers'.
 *
 * @author Kris De Volder
 */
public class Renderables {

	private static final String NO_DESCRIPTION_TEXT = "no description";

	final static Logger logger = LoggerFactory.getLogger(Renderables.class);

	public static final Renderable NO_DESCRIPTION = italic(text(NO_DESCRIPTION_TEXT));
	
	public static Remark getHtmlToMarkdownConverter() {
		return new Remark();
	}
	
	@FunctionalInterface
	public interface HtmlContentFiller {
		void fill(HtmlBuffer buffer);
	}
	
	public static Renderable htmlBlob(HtmlContentFiller contentFiller) {
		return new Renderable() {

			@Override
			public void renderAsHtml(HtmlBuffer buffer) {
				contentFiller.fill(buffer);
			}

			@Override
			public void renderAsMarkdown(StringBuilder buffer) {
				buffer.append(getHtmlToMarkdownConverter().convert(toHtml()));
			}
			
		};
	}

	public static Renderable htmlBlob(String html) {
		return htmlBlob(buffer -> buffer.raw(html));
	}
	
	public static Renderable concat(Renderable... pieces) {
		return concat(ImmutableList.copyOf(pieces));
	}

	public static Renderable concat(List<Renderable> pieces) {
		if (pieces == null || pieces.size() == 0) {
			throw new IllegalArgumentException("At least one hover information is required for concat");
		} else if (pieces.size() == 1) {
			return pieces.get(0);
		} else {
			return new ConcatRenderables(pieces);
		}
	}

	public static Renderable italic(Renderable text) {
		return new Renderable() {

			@Override
			public void renderAsMarkdown(StringBuilder buffer) {
				buffer.append("*");
				text.renderAsMarkdown(buffer);
				buffer.append("*");
			}

			@Override
			public void renderAsHtml(HtmlBuffer buffer) {
				buffer.raw("<i>");
				text.renderAsHtml(buffer);
				buffer.raw("</i>");
			}
		};
	}
	
	public static Renderable paragraph(Renderable text) {
		return new Renderable() {

			@Override
			public void renderAsMarkdown(StringBuilder buffer) {
				buffer.append("\n");
				text.renderAsMarkdown(buffer);
				buffer.append("\n");
			}

			@Override
			public void renderAsHtml(HtmlBuffer buffer) {
				buffer.raw("<p>");
				text.renderAsHtml(buffer);
				buffer.raw("</p>");
			}
		};
	}
	
	public static Renderable strikeThrough(Renderable text) {
		return new Renderable() {
			
			@Override
			public void renderAsMarkdown(StringBuilder buffer) {
				buffer.append("~~");
				text.renderAsMarkdown(buffer);
				buffer.append("~~");
			}

			@Override
			public void renderAsHtml(HtmlBuffer buffer) {
				buffer.raw("<del>");
				text.renderAsHtml(buffer);
				buffer.raw("</del>");
			}
		};
	}

	public static Renderable link(String text, String url) {
		return new Renderable() {

			@Override
			public void renderAsMarkdown(StringBuilder buffer) {
				buffer.append('[');
				buffer.append(text);
				buffer.append(']');
				if (url != null) {
					buffer.append('(');
					buffer.append(url);
					buffer.append(')');
				}
			}

			@Override
			public void renderAsHtml(HtmlBuffer buffer) {
				buffer.raw("<a href=\"");
				buffer.url("" + url);
				buffer.raw("\">");
				buffer.text(text);
				buffer.raw("</a>");
			}
		};
	}

	public static Renderable lineBreak() {
		return new Renderable() {

			@Override
			public void renderAsMarkdown(StringBuilder buffer) {
				if (buffer.charAt(buffer.length() - 1) != '\n') {
					// 2 spaces and then new line would create a line break in text
					buffer.append("  ");
				}
				buffer.append("\n");
			}

			@Override
			public void renderAsHtml(HtmlBuffer buffer) {
				buffer.raw("<br>");
			}
		};
	}

	public static Renderable bold(Renderable text) {

		return new Renderable() {

			@Override
			public void renderAsMarkdown(StringBuilder buffer) {
				buffer.append("**");
				text.renderAsMarkdown(buffer);
				buffer.append("**");
			}

			@Override
			public void renderAsHtml(HtmlBuffer buffer) {
				buffer.raw("<b>");
				text.renderAsHtml(buffer);
				buffer.raw("</b>");
			}
		};
	}

	public static Renderable text(String text) {
		return new Renderable() {
			@Override
			public void renderAsMarkdown(StringBuilder buffer) {
				// TODO: handle escaping
				buffer.append(text);
			}

			@Override
			public void renderAsHtml(HtmlBuffer buffer) {
				buffer.text(text);
			}
		};
	}
	
	public static Renderable lazy(Supplier<Renderable> supplier) {
		return new Renderable() {
			
			@Override
			public void renderAsMarkdown(StringBuilder buffer) {
				supplier.get().renderAsMarkdown(buffer);
			}
			
			@Override
			public void renderAsHtml(HtmlBuffer buffer) {
				supplier.get().renderAsHtml(buffer);
			}
		};
	}

	public static Renderable fromClasspath(final Class<?> klass, final String resourcePath) {
		return new Renderable() {

			@Override
			public void renderAsMarkdown(StringBuilder buffer) {
				String extension = ".md";
				String value = getText(klass, resourcePath, extension);
				if (value != null) {
					buffer.append(value);
				} else {
					NO_DESCRIPTION.renderAsMarkdown(buffer);
				}
			}

			@Override
			public void renderAsHtml(HtmlBuffer buffer) {
				String extension = ".html";
				String value = getText(klass, resourcePath, extension);
				if (value != null) {
					buffer.raw(value);
				} else {
					NO_DESCRIPTION.renderAsHtml(buffer);
				}
			}

			private String getText(final Class<?> klass, final String resourcePath, String extension) {
				try {
					InputStream stream = klass.getResourceAsStream(resourcePath + extension);
					if (stream != null) {
						return IOUtil.toString(stream);
					}
				} catch (Exception e) {
					logger.error("Error", e);
				}
				return null;
			}
		};
	}

	private static class ConcatRenderables implements Renderable {

		private Renderable[] pieces;

		ConcatRenderables(Renderable... pieces) {
			this.pieces = pieces;
		}

		public ConcatRenderables(List<Renderable> pieces) {
			this(pieces.toArray(new Renderable[pieces.size()]));
		}

		@Override
		public void renderAsHtml(HtmlBuffer buffer) {
			for (Renderable hoverInfo : pieces) {
				hoverInfo.renderAsHtml(buffer);
			}
		}

		@Override
		public void renderAsMarkdown(StringBuilder buffer) {
			for (Renderable hoverInfo : pieces) {
				hoverInfo.renderAsMarkdown(buffer);
			}
		}

	}
}
