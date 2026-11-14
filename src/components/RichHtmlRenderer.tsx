import React from 'react';
import { transformEducationalHtml } from '../engine/htmlUtils';

interface RichHtmlRendererProps {
  html: string;
  className?: string;
}

export const RichHtmlRenderer: React.FC<RichHtmlRendererProps> = ({ html, className = '' }) => {
  const safeHtml = React.useMemo(() => transformEducationalHtml(html), [html]);
  return (
    <div
      className={`rich-question-html text-[#1E293B] leading-relaxed overflow-x-auto ${className}`}
      dangerouslySetInnerHTML={{ __html: safeHtml }}
    />
  );
};
