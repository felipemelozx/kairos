'use client';

import { useEffect, useRef, useState } from 'react';
import { Project } from '@/lib/projects-api';

interface ProjectDeleteDialogProps {
  project: Project;
  onCancel: () => void;
  onConfirm: () => Promise<void> | void;
}

const secondaryButton =
  'inline-flex min-h-[40px] items-center justify-center gap-1.5 rounded-xl border-2 border-brand-dark bg-white px-4 py-2.5 text-sm font-bold text-ink shadow-nb-sm transition-all hover:-translate-x-px hover:-translate-y-px hover:shadow-nb-md focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-violet disabled:cursor-not-allowed disabled:opacity-60';
const dangerButton =
  'inline-flex min-h-[40px] items-center justify-center gap-1.5 rounded-xl border-2 border-brand-dark bg-danger px-4 py-2.5 text-sm font-bold text-white shadow-nb-sm transition-all hover:-translate-x-px hover:-translate-y-px hover:shadow-nb-md focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-violet disabled:cursor-not-allowed disabled:opacity-60';

export function ProjectDeleteDialog({ project, onCancel, onConfirm }: ProjectDeleteDialogProps) {
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState('');
  const cancelRef = useRef<HTMLButtonElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    cancelRef.current?.focus();
  }, []);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onCancel();
        return;
      }
      if (event.key !== 'Tab') {
        return;
      }
      const focusables = panelRef.current?.querySelectorAll<HTMLElement>(
        'button:not([disabled])'
      );
      if (!focusables || focusables.length === 0) {
        return;
      }
      const first = focusables[0];
      const last = focusables[focusables.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onCancel]);

  const handleConfirm = async () => {
    setIsDeleting(true);
    setError('');
    try {
      await onConfirm();
    } catch {
      setError('Could not delete the project. Please try again.');
      setIsDeleting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <button
        type="button"
        aria-label="Close delete dialog"
        onClick={onCancel}
        tabIndex={-1}
        className="absolute inset-0 cursor-default bg-ink/40"
      />
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="project-delete-title"
        aria-describedby="project-delete-description"
        className="relative nb-card w-full max-w-sm p-6"
      >
        <h2 id="project-delete-title" className="font-display text-title text-ink">
          Delete project
        </h2>
        <p id="project-delete-description" className="mt-2 text-body text-ink-secondary">
          “{project.name}” will be removed from your projects. This can’t be undone in the app.
        </p>
        {error ? (
          <p role="alert" className="mt-2 text-sm text-danger">
            {error}
          </p>
        ) : null}
        <div className="mt-6 flex items-center justify-end gap-2">
          <button
            ref={cancelRef}
            type="button"
            onClick={onCancel}
            disabled={isDeleting}
            className={secondaryButton}
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={isDeleting}
            className={dangerButton}
          >
            {isDeleting ? 'Deleting...' : 'Delete'}
          </button>
        </div>
      </div>
    </div>
  );
}
