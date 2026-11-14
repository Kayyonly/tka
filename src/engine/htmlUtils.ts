const PUSMENDIK_BASE_URL = 'https://pusmendik.kemendikdasmen.go.id';

export function resolveImageUrl(rawUrl: string): string {
  const trimmed = rawUrl.trim();
  if (!trimmed) return '';
  if (/^(https?:|data:|blob:)/i.test(trimmed)) {
    return trimmed;
  }
  if (trimmed.startsWith('//')) {
    return `https:${trimmed}`;
  }
  try {
    return new URL(trimmed, PUSMENDIK_BASE_URL).toString();
  } catch {
    return trimmed;
  }
}

export function hasMeaningfulHtmlContent(rawHtml?: string | null): boolean {
  if (typeof rawHtml !== 'string') return false;
  if (/<(img|table|svg|math)\b/i.test(rawHtml)) {
    return true;
  }
  const stripped = rawHtml
    .replace(/<\?xml[\s\S]*?\?>/gi, '')
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/\u00a0/g, ' ')
    .trim();
  return stripped.length > 0;
}

/**
 * Sanitizes HTML while preserving educational tags (paragraphs, tables, math,
 * superscript/subscript, lists, and img elements), stripping XML PIs/comments,
 * removing rigid height attributes on images, and resolving relative image src
 * attributes against https://pusmendik.kemendikdasmen.go.id.
 */
export function transformEducationalHtml(rawHtml: string): string {
  const sanitized = rawHtml
    .replace(/<\?xml[\s\S]*?\?>/gi, '')
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
    .replace(/<iframe[\s\S]*?>[\s\S]*?<\/iframe>/gi, '')
    .replace(/\son\w+="[^"]*"/gi, '')
    .replace(/\son\w+='[^']*'/gi, '')
    .replace(/javascript:/gi, '');

  return sanitized.replace(/<img\b([^>]*)>/gi, (_fullMatch, rawAttrs: string) => {
    let attrs = rawAttrs
      .replace(/\sheight=(["'])[^"']*\1/gi, '')
      .replace(/\sstyle=(["'])([^"']*)\1/gi, (_styleMatch, q: string, styleVal: string) => {
        const cleanedStyle = styleVal
          .replace(/(?:^|;)\s*height\s*:[^;]*/gi, '')
          .replace(/^;|;$/g, '')
          .trim();
        return cleanedStyle ? ` style=${q}${cleanedStyle}${q}` : '';
      });

    attrs = attrs.replace(
      /\bsrc=(["'])([^"']*)\1/i,
      (_srcMatch, q: string, src: string) => `src=${q}${resolveImageUrl(src)}${q}`,
    );

    if (!/\breferrerpolicy=/i.test(attrs)) {
      attrs += ' referrerpolicy="no-referrer"';
    }
    if (!/\bloading=/i.test(attrs)) {
      attrs += ' loading="lazy"';
    }

    return `<img${attrs}>`;
  });
}
