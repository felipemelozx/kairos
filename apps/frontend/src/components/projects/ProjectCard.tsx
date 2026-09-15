'use client';

import { useState } from 'react';
import { Project } from '@/lib/projects-api';

interface ProjectCardProps {
  project: Project;
  onEdit: (project: Project) => void;
  onArchive: (project: Project) => Promise<void>;
  onReactivate: (project: Project) => Promise<void>;
  onDelete: (project: Project) => void;
}

const dateFormatter = new Intl.DateTimeFormat('en-US', {
  month: 'short',
  day: 'numeric',
  year: 'numeric',
});

function formatCreatedAt(createdAt: string): string {
  const date = new Date(createdAt);
  if (Number.isNaN(date.getTime())) {
    return 'Created date unavailable';
  }
  try {
    return `Created ${dateFormatter.format(date)}`;
  } catch {
    return 'Created date unavailable';
  }
}

const badgeActive = 'status-badge live';
const badgeArchived = 'status-badge neutral';

const ghostButton =
  'inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border-2 border-brand-dark bg-white px-3 py-2 text-sm font-bold text-ink shadow-nb-sm transition-all hover:-translate-x-px hover:-translate-y-px hover:shadow-nb-md focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-violet disabled:cursor-not-allowed disabled:opacity-60';
const dangerSecondaryButton =
  'inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border-2 border-brand-dark bg-white px-3 py-2 text-sm font-bold text-danger shadow-nb-sm transition-all hover:-translate-x-px hover:-translate-y-px hover:shadow-nb-md focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-violet disabled:cursor-not-allowed disabled:opacity-60';
const primaryButton =
  'inline-flex min-h-[40px] items-center justify-center gap-1.5 rounded-xl border-2 border-brand-dark bg-brand-peach px-4 py-2.5 text-sm font-bold text-ink shadow-nb-sm transition-all hover:-translate-x-px hover:-translate-y-px hover:shadow-nb-md active:translate-x-px active:translate-y-px focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-violet disabled:cursor-not-allowed disabled:opacity-60';
const secondaryButton =
  'inline-flex min-h-[40px] items-center justify-center gap-1.5 rounded-xl border-2 border-brand-dark bg-white px-4 py-2 text-sm font-bold text-ink shadow-nb-sm transition-all hover:-translate-x-px hover:-translate-y-px hover:shadow-nb-md focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-violet disabled:cursor-not-allowed disabled:opacity-60';

export function ProjectCard({
  project,
  onEdit,
  onArchive,
  onReactivate,
  onDelete,
}: ProjectCardProps) {
  const [confirmingArchive, setConfirmingArchive] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [statusError, setStatusError] = useState('');
  const isActive = project.status === 'ACTIVE';

  const handleArchive = async () => {
    setIsUpdating(true);
    setStatusError('');
    try {
      await onArchive(project);
      setConfirmingArchive(false);
    } catch {
      setStatusError('Could not update the project. Please try again.');
    } finally {
      setIsUpdating(false);
    }
  };

  const handleReactivate = async () => {
    setIsUpdating(true);
    setStatusError('');
    try {
      await onReactivate(project);
    } catch {
      setStatusError('Could not update the project. Please try again.');
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <article className="nb-card nb-card-hover flex h-full flex-col gap-4 p-6">
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 items-center gap-2">
          <span
            data-testid="project-color"
            aria-hidden="true"
            className="h-2.5 w-2.5 shrink-0 rounded-pill"
            style={{ backgroundColor: project.color }}
          />
          <span className="sr-only">Project color {project.color}.</span>
          <h2 title={project.name} className="truncate font-display text-title text-ink">
            {project.name}
          </h2>
        </div>
        <span className={isActive ? badgeActive : badgeArchived}>
          {isActive ? 'Active' : 'Archived'}
        </span>
      </div>

      {project.description ? (
        <p className="text-body text-ink-secondary">{project.description}</p>
      ) : null}

      {statusError ? (
        <p role="alert" className="text-sm text-danger">
          {statusError}
        </p>
      ) : null}

      {confirmingArchive ? (
        <div className="nb-card-peach mt-auto p-3">
          <p className="text-body text-ink">Archive “{project.name}”?</p>
          <p className="text-sm text-ink-secondary">You can reactivate it later.</p>
          <div className="mt-3 flex items-center gap-2">
            <button
              type="button"
              onClick={() => setConfirmingArchive(false)}
              disabled={isUpdating}
              className={secondaryButton}
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleArchive}
              disabled={isUpdating}
              className={primaryButton}
            >
              {isUpdating ? 'Archiving...' : 'Archive'}
            </button>
          </div>
        </div>
      ) : (
        <div className="mt-auto flex flex-col gap-3">
          <div className="flex items-center justify-between gap-2">
            <span className="font-mono text-data tabular-nums text-ink-secondary">
              {formatCreatedAt(project.createdAt)}
            </span>
            <span
              title="Total executed time lands with Work Sessions (SESSION-001)"
              className="font-mono text-data tabular-nums text-ink-secondary"
            >
              Executed —h —
            </span>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              id={`project-edit-${project.id}`}
              onClick={() => onEdit(project)}
              aria-label={`Edit ${project.name}`}
              className={secondaryButton}
            >
              <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path
                  d="M11.5 2.5a1.4 1.4 0 0 1 2 2L5 13l-3.5 1L2.5 10.5 11.5 2.5Z"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinejoin="round"
                />
              </svg>
              Edit
            </button>
            {isActive ? (
              <button
                type="button"
                onClick={() => setConfirmingArchive(true)}
                aria-label={`Archive ${project.name}`}
                className={ghostButton}
              >
                <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <rect x="2" y="2.5" width="12" height="9" rx="1" stroke="currentColor" strokeWidth="1.5" />
                  <path d="M2 6h12M6.5 11.5h3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                </svg>
                Archive
              </button>
            ) : (
              <button
                type="button"
                onClick={handleReactivate}
                disabled={isUpdating}
                aria-label={`Reactivate ${project.name}`}
                className={ghostButton}
              >
                {isUpdating ? 'Reactivating...' : 'Reactivate'}
              </button>
            )}
            <button
              type="button"
              onClick={() => onDelete(project)}
              aria-label={`Delete ${project.name}`}
              title="Opens a confirmation dialog. Deletion is permanent in the app."
              className={dangerSecondaryButton}
            >
              <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path
                  d="M2.5 4h11M6.5 2.5h3M4 4l.7 9.2a1 1 0 0 0 1 .8h4.6a1 1 0 0 0 1-.8L12 4"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
              Delete
            </button>
          </div>
        </div>
      )}
    </article>
  );
}
