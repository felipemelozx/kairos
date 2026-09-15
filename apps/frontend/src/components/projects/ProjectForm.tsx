'use client';

import { useRef, useState } from 'react';
import { ApiError } from '@/lib/api';
import { CreateProjectData, Project, UpdateProjectData } from '@/lib/projects-api';
import {
  PROJECT_COLOR_PATTERN,
  PROJECT_COLOR_PRESETS,
  PROJECT_DESCRIPTION_MAX_LENGTH,
  PROJECT_NAME_MAX_LENGTH,
  ProjectErrors,
  ProjectField,
  getFirstInvalidField,
  hasErrors,
  pickFieldErrors,
  validateProject,
} from '@/lib/projects-validation';
import { useProjectsStore } from '@/stores/projects-store';

interface ProjectFormProps {
  mode: 'create' | 'edit';
  project?: Project | null;
  onCancel: () => void;
  onSuccess: () => void;
}

const FIELD_ORDER: ProjectField[] = ['name', 'description', 'color'];

const inputBase =
  'input-nb text-ink focus:outline-none';
const inputDefault = '';
const inputInvalid = 'border-danger';

const primaryButton =
  'inline-flex min-h-[40px] items-center justify-center gap-1.5 rounded-xl border-2 border-brand-dark bg-brand-peach px-4 py-2.5 text-sm font-bold text-ink shadow-nb-sm transition-all hover:-translate-x-px hover:-translate-y-px hover:shadow-nb-md focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-violet disabled:cursor-not-allowed disabled:opacity-60';
const secondaryButton =
  'inline-flex min-h-[40px] items-center justify-center gap-1.5 rounded-xl border-2 border-brand-dark bg-white px-4 py-2.5 text-sm font-bold text-ink shadow-nb-sm transition-all hover:-translate-x-px hover:-translate-y-px hover:shadow-nb-md focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-violet disabled:cursor-not-allowed disabled:opacity-60';

const presetBase = 'h-10 w-10 min-h-[40px] min-w-[40px] rounded-xl border-2 border-brand-dark shadow-nb-sm';
const presetIdle = `${presetBase} transition-all hover:-translate-x-px hover:-translate-y-px hover:shadow-nb-md focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-violet`;
const presetSelected = `${presetBase} ring-2 ring-brand-violet ring-offset-2 ring-offset-white focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-violet`;

export function ProjectForm({
  mode,
  project = null,
  onCancel,
  onSuccess,
}: ProjectFormProps) {
  const { createProject, updateProject, isSubmitting } = useProjectsStore();
  const [name, setName] = useState(project?.name ?? '');
  const [description, setDescription] = useState(project?.description ?? '');
  const [color, setColor] = useState(project?.color ?? '');
  const [fieldErrors, setFieldErrors] = useState<ProjectErrors>({});
  const [formError, setFormError] = useState('');

  const nameRef = useRef<HTMLInputElement>(null);
  const descriptionRef = useRef<HTMLTextAreaElement>(null);
  const colorRef = useRef<HTMLInputElement>(null);
  const fieldRefs = { name: nameRef, description: descriptionRef, color: colorRef } as const;

  const clearFieldError = (field: ProjectField) => {
    setFieldErrors((prev) => {
      if (!prev[field]) {
        return prev;
      }
      return { ...prev, [field]: undefined };
    });
    if (formError) {
      setFormError('');
    }
  };

  const focusFirstInvalid = (errors: ProjectErrors) => {
    const firstInvalid = getFirstInvalidField(errors, FIELD_ORDER);
    if (firstInvalid) {
      fieldRefs[firstInvalid].current?.focus();
    }
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setFormError('');

    const errors = validateProject(
      { name, description, color },
      { requireColor: mode === 'create' }
    );
    setFieldErrors(errors);

    if (hasErrors(errors)) {
      focusFirstInvalid(errors);
      return;
    }

    const trimmedName = name.trim();
    const trimmedDescription = description.trim();
    const trimmedColor = color.trim();

    try {
      if (mode === 'create') {
        const payload: CreateProjectData = {
          name: trimmedName,
          description: trimmedDescription,
          color: trimmedColor,
        };
        await createProject(payload);
      } else {
        if (!project) {
          setFormError('Could not save the project. Please try again.');
          return;
        }
        const changes: UpdateProjectData = {};
        if (trimmedName !== project?.name) {
          changes.name = trimmedName;
        }
        if (trimmedDescription !== (project?.description ?? '')) {
          changes.description = trimmedDescription;
        }
        if (trimmedColor && trimmedColor !== project?.color) {
          changes.color = trimmedColor;
        }
        if (Object.keys(changes).length === 0) {
          onSuccess();
          return;
        }
        await updateProject(project.id, changes);
      }
      onSuccess();
    } catch (error) {
      if (error instanceof ApiError) {
        const details = pickFieldErrors(error.details, FIELD_ORDER);
        if (hasErrors(details)) {
          setFieldErrors(details);
          focusFirstInvalid(details);
          return;
        }
      }
      setFormError(
        error instanceof Error ? error.message : 'Could not save the project. Please try again.'
      );
    }
  };

  const selectedColor = color.trim();
  const colorWellValue = PROJECT_COLOR_PATTERN.test(selectedColor) ? selectedColor : '#0F766E';

  return (
    <section className="nb-card p-6">
      <h2 className="font-display text-title text-ink">
        {mode === 'create' ? 'New project' : 'Edit project'}
      </h2>

      <form onSubmit={handleSubmit} className="mt-4 space-y-4" noValidate aria-busy={isSubmitting}>
        <div>
          <div className="mb-1 flex items-center justify-between gap-2">
            <label htmlFor="project-name" className="text-sm font-medium text-ink-secondary">
              Name
            </label>
            <span className="font-mono text-data text-ink-secondary">
              {name.length}/{PROJECT_NAME_MAX_LENGTH}
            </span>
          </div>
          <input
            ref={nameRef}
            id="project-name"
            name="name"
            type="text"
            autoComplete="off"
            maxLength={PROJECT_NAME_MAX_LENGTH}
            placeholder="e.g. Deep Work"
            value={name}
            onChange={(event) => {
              setName(event.target.value);
              clearFieldError('name');
            }}
            aria-invalid={Boolean(fieldErrors.name)}
            aria-describedby={fieldErrors.name ? 'project-name-error' : undefined}
            className={`${inputBase} ${fieldErrors.name ? inputInvalid : inputDefault}`}
          />
          {fieldErrors.name ? (
            <p id="project-name-error" role="alert" className="mt-1 text-sm text-danger">
              {fieldErrors.name}
            </p>
          ) : null}
        </div>

        <div>
          <div className="mb-1 flex items-center justify-between gap-2">
            <label
              htmlFor="project-description"
              className="text-sm font-medium text-ink-secondary"
            >
              Description (optional)
            </label>
            <span className="font-mono text-data text-ink-secondary">
              {description.length}/{PROJECT_DESCRIPTION_MAX_LENGTH}
            </span>
          </div>
          <textarea
            ref={descriptionRef}
            id="project-description"
            name="description"
            rows={3}
            maxLength={PROJECT_DESCRIPTION_MAX_LENGTH}
            placeholder="What is this project for?"
            value={description}
            onChange={(event) => {
              setDescription(event.target.value);
              clearFieldError('description');
            }}
            aria-invalid={Boolean(fieldErrors.description)}
            aria-describedby={fieldErrors.description ? 'project-description-error' : undefined}
            className={`${inputBase} ${fieldErrors.description ? inputInvalid : inputDefault}`}
          />
          {fieldErrors.description ? (
            <p id="project-description-error" role="alert" className="mt-1 text-sm text-danger">
              {fieldErrors.description}
            </p>
          ) : null}
        </div>

        <fieldset
          aria-describedby={fieldErrors.color ? 'project-color-error' : undefined}
          className="rounded-xl border-2 border-brand-dark p-4"
        >
          <legend className="px-1 text-sm font-medium text-ink-secondary">Color</legend>
          <div className="flex flex-wrap items-center gap-3">
            {PROJECT_COLOR_PRESETS.map((preset) => {
              const pressed = selectedColor.toLowerCase() === preset.toLowerCase();
              return (
                <button
                  key={preset}
                  type="button"
                  aria-label={`Use color ${preset}`}
                  aria-pressed={pressed}
                  onClick={() => {
                    setColor(preset);
                    clearFieldError('color');
                  }}
                  className={pressed ? presetSelected : presetIdle}
                  style={{ backgroundColor: preset }}
                />
              );
            })}
            <input
              type="color"
              aria-label="Pick a custom color"
              value={colorWellValue}
              onChange={(event) => {
                setColor(event.target.value);
                clearFieldError('color');
              }}
              className="h-10 w-10 rounded-xl border-2 border-brand-dark bg-white p-1 shadow-nb-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-violet"
            />
            <div className="min-w-[8rem] flex-1">
              <label htmlFor="project-color" className="block text-sm font-medium text-ink-secondary mb-1">
                Hex color
              </label>
              <input
                ref={colorRef}
                id="project-color"
                name="color"
                type="text"
                inputMode="text"
                maxLength={7}
                placeholder="#0F766E"
                value={color}
                onChange={(event) => {
                  setColor(event.target.value);
                  clearFieldError('color');
                }}
                aria-invalid={Boolean(fieldErrors.color)}
                aria-describedby={fieldErrors.color ? 'project-color-error' : undefined}
                className={`${inputBase} ${fieldErrors.color ? inputInvalid : inputDefault}`}
              />
            </div>
          </div>
          {fieldErrors.color ? (
            <p id="project-color-error" role="alert" className="mt-1 text-sm text-danger">
              {fieldErrors.color}
            </p>
          ) : null}
        </fieldset>

        <p
          id="project-form-error"
          role="alert"
          aria-live="polite"
          className="min-h-[1.25rem] text-sm text-danger"
        >
          {formError}
        </p>

        <div className="flex items-center justify-end gap-2">
          <button type="button" onClick={onCancel} className={secondaryButton}>
            Cancel
          </button>
          <button type="submit" disabled={isSubmitting} className={primaryButton}>
            {isSubmitting ? 'Saving...' : mode === 'create' ? 'Create project' : 'Save changes'}
          </button>
        </div>
      </form>
    </section>
  );
}
